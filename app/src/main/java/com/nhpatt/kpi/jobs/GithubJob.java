package com.nhpatt.kpi.jobs;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.util.Log;

import com.evernote.android.job.Job;
import com.nhpatt.kpi.app.KPIApplication;
import com.nhpatt.kpi.database.KPIOpenHelper;
import com.nhpatt.kpi.models.Commit;
import com.nhpatt.kpi.models.CommitPerYear;
import com.nhpatt.kpi.service.GitHubService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * @author Javier Gamarra
 */
public class GithubJob extends Job {
    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        try {

            List<Commit> commits = loadFromDatabase();
            if (commits.isEmpty()) {
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl("https://api.github.com")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();

                GitHubService service = retrofit.create(GitHubService.class);

                Response<List<Commit>> response = service.commitsPerWeek("nhpatt", "KPI").execute();

                commits = response.body();

                storeCommits(commits);
            }

            EventBus.getDefault().post(new CommitPerYear(commits));
            return Result.SUCCESS;
        } catch (IOException e) {
            Log.e(KPIApplication.TAG, "Error retrieving commits", e);
        }
        return Result.FAILURE;
    }

    @NonNull
    private List<Commit> loadFromDatabase() {
        SQLiteDatabase database = new KPIOpenHelper(getContext()).getReadableDatabase();
        Cursor cursor = database.query(Commit.TABLE_NAME, null, null,
                null, null, null, null, null);
        List<Commit> commits = new ArrayList<>();
        while (cursor.moveToNext()) {
            commits.add(new Commit(cursor.getInt(0), cursor.getInt(1)));
        }
        return commits;
    }

    private void storeCommits(List<Commit> commits) {
        SQLiteDatabase writableDatabase = new KPIOpenHelper(getContext()).getWritableDatabase();

        writableDatabase.beginTransaction();
        for (Commit commit : commits) {
            ContentValues values = new ContentValues();
            values.put(Commit.WEEK, commit.getWeek());
            values.put(Commit.TOTAL, commit.getTotal());
            writableDatabase.insert(Commit.TABLE_NAME, null, values);
        }
        writableDatabase.setTransactionSuccessful();
        writableDatabase.endTransaction();
        writableDatabase.close();
    }
}