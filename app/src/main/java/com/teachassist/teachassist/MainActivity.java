package com.teachassist.teachassist;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.facebook.ads.AdSettings;
import com.google.android.ads.mediationtestsuite.MediationTestSuite;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;

import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;

import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.mopub.common.MoPub;
import com.mopub.common.SdkConfiguration;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.netopen.hotbitmapgg.library.view.RingProgressBar;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    String username;
    String password;

    Boolean Refresh = false;
    SwipeRefreshLayout SwipeRefresh;
    private DrawerLayout drawer;
    LinkedHashMap<String, List<String>> response;
    List<String> removed = new ArrayList<>();
    ProgressDialog dialog;
    NavigationView navigationView;
    Menu menu;
    Context context = (Context) this;
    LinkedList<View> Courses = new LinkedList<View>();
    boolean offlineBannerIsDisplayed = false;
    boolean hasInternetConnection = true;
    private AdView adView;
    private RelativeLayout adContainerView;
    private boolean isPremiumUser = false;


    ArrayList<Integer> removedCourseIndexes = new ArrayList<>();
    Boolean isEditing = false;

    public static final String CREDENTIALS = "credentials";
    public static final String USERNAME = "USERNAME";
    public static final String PASSWORD = "PASSWORD";
    public static final String REMEMBERME = "REMEMBERME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if(sharedPreferences.getBoolean("lightThemeEnabled", true)){
            setTheme(R.style.LightTheme);
        }else{
            setTheme(R.style.DarkTheme);
        }
        if(sharedPreferences.getBoolean("isPremiumUser", false)){
            isPremiumUser = true;
        }
        setContentView(R.layout.activity_main);
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w("test", "getInstanceId failed", task.getException());
                            return;
                        }

                        // Get new Instance ID token
                        String token = task.getResult().getToken();

                        System.out.println("TOKEN "+token);
                    }
                });

        if(isPremiumUser) {
            findViewById(R.id.adViewContainer).setVisibility(View.GONE);
        }else{
            // initilize ads
            //AdSettings.addTestDevice("27a5d5ac-8abd-428e-a41a-33e9ef0c3f32");
            //new RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("3FF2A492DE30FB700E4734A350C51D69"));

            //MoPub main view banner
            SdkConfiguration sdkConfiguration =
                    new SdkConfiguration.Builder("db7e11922adc40218eb92998315fbd50").build();
            MoPub.initializeSdk(context, sdkConfiguration, null);

            //MoPub assignments view banner
            SdkConfiguration sdkConfigurationAssignments =
                    new SdkConfiguration.Builder("90442790687e4d56975f4a1141d5e32c").build();

            MoPub.initializeSdk(context, sdkConfigurationAssignments, null);

            //AdMob
            MobileAds.initialize(this, new OnInitializationCompleteListener() {
                @Override
                public void onInitializationComplete(InitializationStatus initializationStatus) {
                }
            });
            adContainerView = findViewById(R.id.adViewContainer);
            // Step 1 - Create an AdView and set the ad unit ID on it.
            adView = new AdView(this);
            adView.setAdUnitId("ca-app-pub-6294253616632635/8914209983");
            //adView.setAdUnitId("ca-app-pub-3940256099942544/6300978111"); //TEST ADS
            adContainerView.addView(adView);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) adView.getLayoutParams();
            params.addRule(RelativeLayout.CENTER_HORIZONTAL);
            adView.setLayoutParams(params);
            loadBanner();

        }


        //progress dialog
        dialog = ProgressDialog.show(MainActivity.this, "",
                "Loading...", true);
        Typeface typeface = ResourcesCompat.getFont(this, R.font.roboto_mono);


        //intent
        Intent intent = getIntent();
        username = intent.getStringExtra("username");
        password = intent.getStringExtra("password");
        doTasksOnFirstLaunch();

        Crashlytics.setUserIdentifier(username);
        Crashlytics.setString("username", username);
        Crashlytics.setString("password", password);
        Crashlytics.log(Log.DEBUG, "username", username);
        Crashlytics.log(Log.DEBUG, "password", password);



        // Refresh
        SwipeRefresh = findViewById(R.id.swipeRefresh);
        SwipeRefresh.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        Log.i("REFRESH", "onRefresh called from MainActivity");

                        // This method performs the actual data-refresh operation.
                        // The method calls setRefreshing(false) when it's finished.
                        Refresh = true;

                        LinearLayout linearLayout = findViewById(R.id.CourseLinearLayout);
                        for(View rl: Courses) {
                            linearLayout.removeViewAt(3);
                        }
                        removedCourseIndexes = new ArrayList<>();
                        isEditing = false;
                        Courses = new LinkedList<View>();
                        new getCoursesInBackground().execute();

                    }
                }
        );



        //setup toolbar for nav bar drawer
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        // Nav bar Drawer
        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // Setup toolbar text
        TextView ToolbarText =  findViewById(R.id.toolbar_title);
        ToolbarText.setText("Student: "+ username);
        getSupportActionBar().setTitle("");
        //getSupportActionBar().setTitle("Student: " + username);

        new getCoursesInBackground().execute();
    }


    private void showToast(String text){
        Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
    }

    private void loadBanner() {
        // Create an ad request. Check your logcat output for the hashed device ID
        // to get test ads on a physical device, e.g.,
        // "Use AdRequest.Builder.addTestDevice("ABCDE0123") to get test ads on this
        // device."
        AdRequest adRequest =
                new AdRequest.Builder()
                        .build();

        AdSize adSize = getAdSize();
        // Step 4 - Set the adaptive ad size on the ad view.
        adView.setAdSize(adSize);

        // Step 5 - Start loading the ad in the background.
        adView.loadAd(adRequest);
    }

    private AdSize getAdSize() {
        // Step 2 - Determine the screen width (less decorations) to use for the ad width.
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;

        int adWidth = (int) (widthPixels / density);

        // Step 3 - Get adaptive ad size and return for setting on the ad view.
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
    }


    public class subjectClick implements View.OnClickListener{

        @Override
        public void onClick(View v){
            Intent myIntent = new Intent(MainActivity.this, CourseInfoActivity.class);
            int subjectNumber = ((LinearLayout) v.getParent()).indexOfChild(v) -3; //this minus three accounts for the toolbar, offline banner and index offset
            int toSubtract = 0;
            for (int i : removedCourseIndexes) {
                if (i < subjectNumber) {
                    toSubtract++;
                }
            }
            TextView p = v.findViewById(R.id.Period);
            subjectNumber -= toSubtract;
            String subjectMark = "";
            String courseCode = "";
            int counter = 0;
            for (Map.Entry<String, List<String>> entry : response.entrySet()) {
                if(counter == subjectNumber){
                    subjectMark = entry.getValue().get(0);
                    courseCode = entry.getValue().get(1);
                }
                counter++;
            }
            myIntent.putExtra("username", username);
            myIntent.putExtra("password", password);
            myIntent.putExtra("subjectNumber", subjectNumber + toSubtract);
            myIntent.putExtra("subject Mark", subjectMark);
            myIntent.putExtra("courseCode", courseCode);
            //myIntent.putExtra()
            Crashlytics.log(Log.DEBUG, "subject Mark", subjectMark);
            startActivity(myIntent);
            dialog.dismiss();


        }
    }

    public class onTrashClick implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            TA ta = new TA();
            Double newAverage = -1.0;
            View rlParent = (View) v.getParent().getParent();
            String toRemove = "";
            rlParent.setVisibility(View.GONE);
            int toSubtract = 0;
            int courseNum = 0;

            int count = 0;
            for(View rl: Courses) {
                if (rlParent == rl) {
                    courseNum = count;
                }
                count++;
            }


            for (int i : removedCourseIndexes) {
                if (i < courseNum) {
                    toSubtract++;
                }
            }
            int counter = 0;
            for (Map.Entry<String, List<String>> entry : response.entrySet()) {
                if (counter == courseNum - toSubtract) {
                    toRemove = entry.getKey();
                }
                counter++;
            }
            removedCourseIndexes.add(courseNum);
            response.remove(toRemove);
            newAverage = ta.GetSemesterAverage(response);
            TextView AverageInt = findViewById(R.id.semesterAverageTV);
            AverageInt.setText(newAverage.toString() + "%");
            final RingProgressBar ProgressBarAverage = (RingProgressBar) findViewById(R.id.AverageBar);
            if (newAverage < 1) {
                ProgressBarAverage.setProgress(1);
            } else {
                ProgressBarAverage.setProgress((int) Math.round(newAverage));
            }

        }
    }



    // on navigation drawer item selection
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch(menuItem.getItemId()){
            case R.id.nav_logout:
                drawer.closeDrawer(GravityCompat.START);


                SharedPreferences sharedPreferences = getSharedPreferences(CREDENTIALS, MODE_PRIVATE);
                SharedPreferences.Editor editor =   sharedPreferences.edit();
                editor.putString(USERNAME, "");
                editor.putString(PASSWORD, "");
                editor.putBoolean(REMEMBERME, false);
                editor.apply();

                //logout of firebase
                FirebaseAuth.getInstance().signOut();

                /*
                //register for notifications if not already registered
                SharedPreferences sharedPreferencesNotifications = getSharedPreferences("notifications", MODE_PRIVATE);
                String token = sharedPreferencesNotifications.getString("token", "");

                if(!token.equals("")) {
                    try {
                        JSONObject json = new JSONObject();
                        json.put("purpose", "delete");
                        json.put("token", token);
                        json.put("auth", "TAAPPYRDSB123!PASSWORD");
                        new unregisterFromNotificationServer().execute(json);
                        SharedPreferences.Editor editorNotifications =   sharedPreferencesNotifications.edit();
                        editorNotifications.putBoolean("hasRegistered", false);
                        editorNotifications.apply();
                    }catch (Exception e){}

                }*/

                Intent myIntent = new Intent(MainActivity.this, login.class);
                startActivity(myIntent);
                finish();
                break;

            case R.id.nav_settings:
                drawer.closeDrawer(GravityCompat.START);
                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settingsIntent);
                break;

            case R.id.nav_bug_report:
                Auth auth1 = new Auth();
                drawer.closeDrawer(GravityCompat.START);
                String mailtoBug = "mailto:taappyrdsb@gmail.com" +
                        "?subject=" + Uri.encode("") +
                        "&body=" + Uri.encode(" \n \n Support Token (please do not delete): \n" +
                        auth1.getSupportToken(username, password));

                Intent BugIntent = new Intent(Intent.ACTION_SENDTO);
                BugIntent.setData(Uri.parse(mailtoBug));

                try {
                    startActivity(BugIntent);
                } catch (ActivityNotFoundException e) {
                    showToast("No email app currently installed");
                }
                break;

            case R.id.nav_release_notes:
                showReleaseNotesDialogIfFirstLaunch(true);
                drawer.closeDrawer(GravityCompat.START);
                break;

            case R.id.nav_open_in:
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setData(Uri.parse("https://ta.yrdsb.ca/yrdsb/"));
                startActivity(intent);
                break;
        }
        return false;
    }

    //2 methods below for edit button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_button, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit:
                if(!isEditing) {
                    for(View rl: Courses) {
                        ImageButton trash = rl.findViewById(R.id.trash_can);
                        trash.setVisibility(View.VISIBLE);
                        trash.setOnClickListener(new onTrashClick());
                    }
                    isEditing = true;
                }else{
                    for(View rl: Courses) {
                        ImageButton trash = rl.findViewById(R.id.trash_can);
                        trash.setVisibility(View.GONE);
                        trash.setOnClickListener(new onTrashClick());
                    }
                    isEditing = false;
                }

                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }


    //close drawer when back button pressed
    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    // dismisses dialog so when user resume is smooth
    public void onPause() {
        super.onPause();
        dialog.dismiss();

    }
    public void onResume() {
        super.onResume();
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if(sharedPreferences.getBoolean("isPremiumUser", false)){
            isPremiumUser = true;
            findViewById(R.id.adViewContainer).setVisibility(View.GONE);
            //Toast.makeText(context, "Your Purchase Has Been Restored", Toast.LENGTH_LONG).show();
        }
    }

    private class getCoursesInBackground extends AsyncTask<String, Integer, LinkedHashMap<String, List<String>>>{
        TA ta = new TA();
        @Override
        protected LinkedHashMap<String, List<String>> doInBackground(String... params){


            try {
                AdvertisingIdClient.Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(MainActivity.this.getApplicationContext());
                System.out.println(adInfo);
                System.out.println("ADVERTISING ID ABOVE ^^");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (GooglePlayServicesNotAvailableException e) {
                e.printStackTrace();
            } catch (GooglePlayServicesRepairableException e) {
                e.printStackTrace();
            }


            response = ta.GetCoursesHTML(username, password);
            LinkedHashMap<String, List<String>> offlineResponse = ta.GetCoursesOffline(username, getApplicationContext());
            if(response == null && offlineResponse.size() == 0){
                hasInternetConnection = false;
                return null;
            }else if(response == null){
                hasInternetConnection = false;
                response = offlineResponse;
                return response;
            }else {
                //this disgusting block of code just merges the 2 responses so that the online response takes precidence
                LinkedHashMap<String, List<String>> mergedResponse = new LinkedHashMap<>();
                int courseNumberResponse = 0;
                for (Map.Entry<String, List<String>> entry : ((LinkedHashMap<String, List<String>>) response.clone()).entrySet()) {
                    if (entry.getKey().contains("NA")) {
                        if(offlineResponse.size() == 0){
                            mergedResponse.put(entry.getKey(), entry.getValue());
                        }else {
                            int courseNumberOfflineResponse = 0;
                            for (Map.Entry<String, List<String>> entryOffline : offlineResponse.entrySet()) {
                                if (courseNumberResponse == courseNumberOfflineResponse) {
                                    if (entryOffline.getKey().contains("NA")) {
                                        mergedResponse.put("NA" + courseNumberResponse, entryOffline.getValue());
                                    } else if (entryOffline.getKey().contains("offline")) {
                                        mergedResponse.put("offline" + courseNumberResponse, entryOffline.getValue());
                                    }
                                }
                                courseNumberOfflineResponse++;
                            }
                        }
                    }else{
                        mergedResponse.put(entry.getKey(), entry.getValue());
                    }
                    courseNumberResponse++;
                }
            /*ArrayList list1 = new ArrayList<>(Arrays.asList("64.2", "AVI3M1-01", "Visual Arts", "169"));
            response = new LinkedHashMap<>();
            response.put("283098", list1);*/
            /*
            //register for notifications if not already registered
            final SharedPreferences sharedPreferencesNotifications = getSharedPreferences("notifications", MODE_PRIVATE);
            String token = sharedPreferencesNotifications.getString("token", "");
            boolean hasRegistered = sharedPreferencesNotifications.getBoolean("hasRegistered", false);
            System.out.println(hasRegistered +"HAS REGISTERED");
            if(!token.equals("") && !hasRegistered) {
                try {
                    JSONObject json = new JSONObject();
                    json.put("username", username);
                    json.put("password", password);
                    json.put("purpose", "register");
                    json.put("token", token);
                    json.put("platform", "ANDROID");
                    json.put("auth", "TAAPPYRDSB123!PASSWORD");
                    SendRequest sr = new SendRequest();
                    if(sr.sendJsonNotifications("https://benjamintran.me/TeachassistAPI/", json.toString()) != null){
                        SharedPreferences.Editor editorNotifications =   sharedPreferencesNotifications.edit();
                        editorNotifications.putBoolean("hasRegistered", true);
                        editorNotifications.apply();
                    }

                }catch (Exception e){}
            } else if(token.equals("")){
                FirebaseInstanceId.initializeInstance().getInstanceId()
                        .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                            @Override
                            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                                if (!task.isSuccessful()) {
                                    return;
                                }
                                // Get new Instance ID token
                                String token = task.getResult().getToken();

                                SharedPreferences.Editor editorNotifications =   sharedPreferencesNotifications.edit();
                                editorNotifications.putString("token", token);
                                editorNotifications.apply();
                            }
                        });
            }
            */
            response = mergedResponse;
            System.out.println(response);
                return response;
            }
        }
        protected void onPostExecute(LinkedHashMap<String, List<String>> response) {
            // Set Average Text
            Double average = ta.GetSemesterAverage(response);
            //check if connected to internet
            if(average == null || response == null){
                new AlertDialog.Builder(context)
                        .setTitle("Connection Error")
                        .setMessage("Something went wrong while trying to reach TeachAssist. Please check your internet connection and try again.")
                        .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d("MainActivity", "No internet connection");
                                recreate();
                            }
                        })
                        .show();
                dialog.dismiss();
                return;
            }
            if(response.size() == 0){
                findViewById(R.id.noCoursesTV).setVisibility(View.VISIBLE);
                TextView AverageInt = findViewById(R.id.semesterAverageTV);
                AverageInt.setText("");
            }else {
                TextView semesterAverageTV = findViewById(R.id.semesterAverageTV);
                semesterAverageTV.setText(average + "%");
            }

            int periodNum = 1;
            LinearLayout linearLayout = findViewById(R.id.CourseLinearLayout);
            for (Map.Entry<String, List<String>> entry : response.entrySet()) {
                View relativeLayout = LayoutInflater.from(context).inflate(R.layout.course_layout, null);
                linearLayout.addView(relativeLayout);
                relativeLayout.setOnClickListener(new subjectClick());
                String markString = "Please See Teacher For Current Mark In This Course";
                String courseCode = "";
                String courseName = "";
                String roomNumber = "";
                List<String> courseData = entry.getValue();
                if (entry.getKey().contains("NA")) {
                    courseCode = getOrBlank(courseData, 0);
                    courseName = getOrBlank(courseData, 1);
                    roomNumber = getOrBlank(courseData, 2);
                    final View ProgressBarAverage = relativeLayout.findViewById(R.id.SubjectBar);
                    ProgressBarAverage.setVisibility(View.INVISIBLE);
                    relativeLayout.setClickable(false);
                } else {
                    try {
                        float mark = Float.parseFloat(entry.getValue().get(0));
                        markString = mark == 100.0 ? "100%" : (mark + "%");
                    } catch (Exception ignored) {}
                    courseCode = getOrBlank(courseData, 1);
                    courseName = getOrBlank(courseData, 2);
                    roomNumber = getOrBlank(courseData, 3);
                }
                if(entry.getKey().contains("offline")){
                    View offlineIndicator = relativeLayout.findViewById(R.id.offlineIndicator);
                    offlineIndicator.setVisibility(View.VISIBLE);
                    if(!offlineBannerIsDisplayed) {
                        final RelativeLayout offlineIndicatorBanner = findViewById(R.id.offlineIndicatorBanner);
                        if(hasInternetConnection) {
                            ((TextView) offlineIndicatorBanner.findViewById(R.id.offlineIndicatorBannerTV))
                                    .setText("Some courses are currently hidden, they may not be up to date.");
                        }
                        offlineIndicatorBanner.setVisibility(View.VISIBLE);
                        offlineBannerIsDisplayed = true;
                        new Thread(new Runnable() {
                            public void run() {
                                try {
                                    Thread.sleep(10000);
                                    offlineIndicatorBanner.post(new Runnable() {
                                        public void run() {
                                            ObjectAnimator animation = ObjectAnimator.ofFloat(offlineIndicatorBanner, "translationX", offlineIndicatorBanner.getWidth());
                                            animation.setDuration(700);
                                            animation.start();
                                        }
                                    });
                                    Thread.sleep(1000);
                                    offlineIndicatorBanner.post(new Runnable() {
                                        public void run() {
                                            offlineIndicatorBanner.setVisibility(View.INVISIBLE);
                                        }
                                    });
                                }catch (Exception ignored){}
                            }
                        }).start();
                    }
                }
                // Write all the individual attributes of a course like the average, courseName etc. to the database
                // Android requires that the action be performed within a thread
                final String finalMarkString = markString;
                //we strip the spaces when we pass this variable to the assignment view which causes
                // an issue if we dont strip it here too since this is the primary key
                final String finalCourseCode = courseCode;
                final String finalCourseName = courseName;
                final String finalRoomNumber = roomNumber;
                final int finalPeriodNum = periodNum;
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                                AppDatabase.class, username).build();
                        CoursesEntity courseEntity = db.coursesDao().getCourseByCourseCode(finalCourseCode.replaceAll("\\s+", ""));
                        if(courseEntity == null){
                            courseEntity = new CoursesEntity();
                        }
                        if(!finalMarkString.equals("Please See Teacher For Current Mark In This Course")) {
                            courseEntity.average = Double.parseDouble(finalMarkString.replace("%", ""));
                            courseEntity.subjectID = "offline"+finalPeriodNum;
                            if(courseEntity.assignments == null){
                                JSONObject returnValue = ta.GetAssignmentsHTML(finalPeriodNum-1);
                                if(returnValue != null) {
                                    courseEntity.assignments = returnValue.toString();
                                }
                            }
                        }
                        if(courseEntity.average == null){
                            courseEntity.subjectID = "NA"+finalPeriodNum; //this means
                            //coursesEntity.average = 2.0; //debugging code
                        }
                        //If there exists an actual subjectID that is not NA, it should not get overwritten because that unhidden course will have been stored in the database with its Subject ID
                        courseEntity.periodNumber = finalPeriodNum;
                        courseEntity.courseCode = finalCourseCode.replaceAll("\\s+", "");
                        courseEntity.courseName = finalCourseName;
                        courseEntity.roomNumber = finalRoomNumber;
                        if(db.coursesDao().getCourseByCourseCode(finalCourseCode.replaceAll("\\s+", "")) != null){ //exists
                            db.coursesDao().updateCourse(courseEntity);
                        }else { //does not exist
                            db.coursesDao().insertAll(courseEntity);
                        }
                        db.close();
                    }
                });

                //update layout
                TextView SubjectAbrv = relativeLayout.findViewById(R.id.SubjectAbrv);
                if(courseCode.contains("SHAL")){ // this is almost always contained within the spare course code ex: SHAL.1-03
                    SubjectAbrv.setText("Spare");
                }else{
                    SubjectAbrv.setText(courseCode);
                }
                TextView SubjectName = relativeLayout.findViewById(R.id.SubjectName);
                SubjectName.setText(courseName);
                TextView subjectInt = relativeLayout.findViewById(R.id.SubjectInt);
                subjectInt.setText(markString);
                if(markString.equals("Please See Teacher For Current Mark In This Course")){
                    subjectInt.setTextSize(14);
                }
                TextView period = relativeLayout.findViewById(R.id.Period);
                period.setText("Period " + periodNum);
                if (!roomNumber.equals("")) {
                    TextView roomNumberText = relativeLayout.findViewById(R.id.RoomNumber);
                    roomNumberText.setText("— Rm " + roomNumber);
                }

                Courses.add(relativeLayout);
                periodNum++;
            }

            dialog.dismiss();
            showReleaseNotesDialogIfFirstLaunch(false);
            RunTasks(response);
        }


        private void RunTasks(final LinkedHashMap<String, List<String>> response){
            final int animationTimeInMs = 10;
           //animate overall average
            new Thread(new Runnable(){
                public void run() {
                    TA ta = new TA();
                    int roundedAvg = (int) Math.round(ta.GetSemesterAverage(response));
                    final RingProgressBar ProgressBarAverage = findViewById(R.id.AverageBar);

                    try {
                        for (int i = 0; i < roundedAvg; i += 2) {
                            ProgressBarAverage.setProgress(i);
                            Thread.sleep(animationTimeInMs);
                        }
                        ProgressBarAverage.setProgress(roundedAvg);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            //animate subjects
            int currentSubject = 0;
            for (final Map.Entry<String, List<String>> entry : response.entrySet()) {
                final int thisSubject=currentSubject;
                currentSubject++;

                new Thread(new Runnable(){
                    public void run() {
                        //get view & average
                        final RingProgressBar ProgressBarAverage =
                                Courses.get(thisSubject).findViewById(R.id.SubjectBar);
                        if(ProgressBarAverage.getVisibility() != View.INVISIBLE) {
                            ProgressBarAverage.setVisibility(View.VISIBLE);
                        }
                        int roundedCourseAvg = 0;
                        if (!entry.getKey().contains("NA")) {
                            roundedCourseAvg = (int) Float.parseFloat(entry.getValue().get(0));
                        }

                        //animate
                        try {
                            for (int i = 0; i < roundedCourseAvg; i += 2) {
                                ProgressBarAverage.setProgress(i);
                                Thread.sleep(animationTimeInMs);
                            }
                            ProgressBarAverage.setProgress(roundedCourseAvg);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }

            if(Refresh.equals(true)) {
                SwipeRefresh.setRefreshing(false);
                Refresh = false;
            }
        }
    }

    private void showReleaseNotesDialogIfFirstLaunch(boolean showIfNotFirstLaunch){
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        if(!sharedPreferences.getBoolean("release notes 3.3.7", false) || showIfNotFirstLaunch) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("release notes 3.3.7", true);
            editor.putBoolean("didAskForRating", false); //reset the value from last popup
            editor.apply();
            // custom popup dialog
            final Dialog dialog = new Dialog(context);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setContentView(R.layout.custom_dialog);
            Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonOK);
            // if button is clicked, close the custom dialog
            dialogButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            Button dialogButtonRate = (Button) dialog.findViewById(R.id.dialogButtonRate);
            // if button is clicked, close the custom dialog
            dialogButtonRate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CheckIfUserIsPremium userIsPremiumClass = new CheckIfUserIsPremium();
                    userIsPremiumClass.check(context, MainActivity.this, true);
                    dialog.dismiss();
                }
            });
            dialog.show();
        }
    }
    private void doTasksOnFirstLaunch(){
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        if(!sharedPreferences.getBoolean("4.0.9 didClearDB", false)) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("4.0.9 didClearDB", true);
            editor.apply();
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                            AppDatabase.class, username).build();
                    db.clearAllTables();
                    db.close();
                    System.out.println("CLEARED ALL DATABASES");
                }
            });

        }
    }
    /*private class unregisterFromNotificationServer extends AsyncTask<JSONObject, Object, Object[]> {
        @Override
        protected void onPreExecute(){
            super.onPreExecute();
        }

        @Override
        protected Object[] doInBackground(JSONObject... params){
            try {
                //SendRequest sr = new SendRequest();
                //sr.sendJsonNotifications("https://benjamintran.me/TeachassistAPI/", params[0].toString());
            }catch (Exception e){}
            return null;

        }

        protected void onProgressUpdate(Object... params) {

        }

        protected void onPostExecute(Object... params) {


        }
    }*/

    private String getOrBlank(List<String> list,int index){
        try {
            return list.get(index);
        }catch (Exception ignored){
            return "";
        }
    }

}
