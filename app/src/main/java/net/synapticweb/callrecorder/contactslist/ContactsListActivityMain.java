package net.synapticweb.callrecorder.contactslist;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;
import android.provider.ContactsContract;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import net.synapticweb.callrecorder.BuildConfig;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.TemplateActivity;
import net.synapticweb.callrecorder.HelpActivity;
import net.synapticweb.callrecorder.settings.SettingsActivity;
import net.synapticweb.callrecorder.setup.SetupActivity;


public class ContactsListActivityMain extends TemplateActivity {
    private static final String TAG = "CallRecorder";
    private static final int REQUEST_PHONE_NUMBER = 1;
    private static final int SETUP_ACTIVITY = 3;
    public static final String HAS_RUN_ONCE = "has_run_once";
    public static final int IS_FIRST_RUN = 1;
    public static final int PERMS_NOT_GRANTED = 2;
    public static final int POWER_OPTIMIZED = 4;
    public static final String SETUP_ARGUMENT = "setup_arg";

    @Override
    protected Fragment createFragment() {
            return new ContactsListFragment();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkIfThemeChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_masterdetail);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayShowTitleEnabled(false);

        TextView title = findViewById(R.id.actionbar_title);
        title.setText(getString(R.string.app_name));
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        int isFirstRun = settings.getBoolean(HAS_RUN_ONCE, false) ? 0 : IS_FIRST_RUN;
        int permsNotGranted = 0, powerOptimized = 0;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permsNotGranted = checkPermissions() ? 0 : PERMS_NOT_GRANTED;
            if(pm != null)
                powerOptimized = pm.isIgnoringBatteryOptimizations(getPackageName()) ? 0 : POWER_OPTIMIZED;
        }
        int checkResult = isFirstRun | permsNotGranted | powerOptimized;

        if(checkResult != 0) {
            Intent setupIntent = new Intent(this, SetupActivity.class);
            setupIntent.putExtra(SETUP_ARGUMENT, checkResult);
            startActivityForResult(setupIntent, SETUP_ACTIVITY);
        }

        insertFragment(R.id.contacts_list_fragment_container);

        FloatingActionButton fab = findViewById(R.id.add_numbers);
        fab.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v)
            {
                Intent pickNumber = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                startActivityForResult(pickNumber, REQUEST_PHONE_NUMBER);
            }
        });

        Button hamburger = findViewById(R.id.hamburger);
        final DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);

        int navWidth;
        int pixelsDp = (int) (getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density);
        navWidth = (pixelsDp >= 480) ? (int) (getResources().getDisplayMetrics().widthPixels * 0.4) :
                (int) (getResources().getDisplayMetrics().widthPixels * 0.8);

        DrawerLayout.LayoutParams params = (DrawerLayout.LayoutParams) navigationView.getLayoutParams();
        params.width = navWidth;
        navigationView.setLayoutParams(params);

        hamburger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawer.openDrawer(GravityCompat.START);
            }
        });

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.settings: startActivity(new Intent(ContactsListActivityMain.this, SettingsActivity.class));
                        break;
                    case R.id.help: startActivity(new Intent(ContactsListActivityMain.this, HelpActivity.class));
                        break;
                    case R.id.about:
                        showAboutDialog();
                        break;
                }
                drawer.closeDrawers();
                return true;
            }
        });
    }

    private void showAboutDialog() {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .customView(R.layout.about_dialog, false)
                .positiveText(android.R.string.ok).build();

        TextView aboutAppName = (TextView) dialog.findViewById(R.id.about_appname);
        aboutAppName.setText(String.format(getResources().getString(R.string.about_appname_version),
                getResources().getString(R.string.app_name),
                BuildConfig.VERSION_NAME));
        dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data); //necesar pentru că altfel nu apelează onActivityResult din fragmente:
        // https://stackoverflow.com/questions/6147884/onactivityresult-is-not-being-called-in-fragment
        if(resultCode == RESULT_OK && requestCode == SETUP_ACTIVITY) {
            if(data.getBooleanExtra(SetupActivity.EXIT_APP, true))
                finish();
        }
    }

    @Override
    public void onBackPressed() {
        new MaterialDialog.Builder(this)
                .title(R.string.exit_app_title)
                .icon(getResources().getDrawable(R.drawable.question_mark))
                .content(R.string.exit_app_message)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        ContactsListActivityMain.super.onBackPressed();
                    }
                })
                .show();
    }

    private boolean checkPermissions() {
        boolean outgoingCalls = ContextCompat.checkSelfPermission(this, Manifest.permission.PROCESS_OUTGOING_CALLS)
                == PackageManager.PERMISSION_GRANTED;
        boolean phoneState = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED;
        boolean callLog = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
                == PackageManager.PERMISSION_GRANTED;
        boolean recordAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
        boolean readContacts = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
        boolean readStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        boolean writeStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;

        return outgoingCalls && phoneState && callLog && recordAudio && readContacts && readStorage && writeStorage;
    }

}