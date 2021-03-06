package com.nhpatt.kpi;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.evernote.android.job.JobRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.nhpatt.kpi.adapters.SmartFragmentStatePagerAdapter;
import com.nhpatt.kpi.app.KPIApplication;
import com.nhpatt.kpi.fragments.FilmsFragment;
import com.nhpatt.kpi.fragments.GithubFragment;
import com.nhpatt.kpi.fragments.Notifiable;
import com.nhpatt.kpi.fragments.ShowsFragment;
import com.nhpatt.kpi.models.CommitPerYear;
import com.nhpatt.kpi.models.Film;
import com.nhpatt.kpi.models.XML;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;

public class DashboardActivity extends AppCompatActivity
        implements HasFilms, HasCommits, HasXML, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final int GITHUB_VIEW = 0;
    public static final int SHOWS_VIEW = 1;
    public static final int FILMS_VIEW = 2;

    @Bind(R.id.view_pager)
    public ViewPager viewPager;

    private XML xml;
    private CommitPerYear commitPerYear;
    private ViewPagerAdapter viewPagerAdapter;
    private List<Film> films = new ArrayList<>();
    private GoogleApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        ButterKnife.bind(this);

        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();

        EventBus.getDefault().register(this);

//        launchJob("github");
        launchJob("shows");
        requestFilms();

//        retrieveGPSLocation();

//        retrieveGPSbyFusedLocation();
    }

    private void retrieveGPSbyFusedLocation() {
        apiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        apiClient.connect();
    }

    private void retrieveGPSLocation() {
        final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (location != null) {
            Log.d(KPIApplication.TAG, location.toString());
        }

        final LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.d(KPIApplication.TAG, location.toString());
                locationManager.removeUpdates(this);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    private void launchJob(String job) {
        new JobRequest.Builder(job)
                .setExact(1L)
                .build()
                .schedule();
    }

    private void requestFilms() {
        if (!isNetworkAvailable()) {
            List<Film> films = Film.listAll(Film.class);
            EventBus.getDefault().post(films);
        } else {
            launchJob("films");
        }
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);

        super.onPause();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void onEventMainThread(CommitPerYear commitPerYear) {
        this.commitPerYear = commitPerYear;

        notifyFragment(GITHUB_VIEW);
    }

    public void onEventMainThread(XML xml) {
        this.xml = xml;

        notifyFragment(SHOWS_VIEW);
    }

    public void onEventMainThread(List<Film> films) {
        this.films = films;

        notifyFragment(FILMS_VIEW);
    }

    private void notifyFragment(int item) {
        Notifiable fragment = (Notifiable) viewPagerAdapter.getRegisteredFragment(item);
        if (fragment != null) {
            fragment.notifyEvent();
        }
    }

    @Override
    public CommitPerYear getCommits() {
        return commitPerYear;
    }

    @Override
    public XML getXML() {
        return xml;
    }

    @Override
    public List<Film> getFilms() {
        return films;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Location location = LocationServices.FusedLocationApi.getLastLocation(apiClient);
        if (location != null) {
            Log.d(KPIApplication.TAG, location.toString());
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private class ViewPagerAdapter extends SmartFragmentStatePagerAdapter {

        public ViewPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case GITHUB_VIEW:
                    return GithubFragment.newInstance();
                case SHOWS_VIEW:
                    return ShowsFragment.newInstance();
                case FILMS_VIEW:
                    return FilmsFragment.newInstance();
                default:
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "Page " + position;
        }
    }
}