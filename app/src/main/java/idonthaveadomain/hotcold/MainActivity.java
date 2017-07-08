package idonthaveadomain.hotcold;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.location.LocationServices;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends Activity implements ConnectionCallbacks
{
    private double latitude, longitude;
    private GoogleApiClient client;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());

        ((Spinner) findViewById(R.id.spnRadius)).setAdapter(ArrayAdapter.createFromResource(this,
                R.array.radius_array,
                android.R.layout.simple_spinner_dropdown_item));

        ((Spinner) findViewById(R.id.spnType)).setAdapter(ArrayAdapter.createFromResource(this,
                R.array.type_array,
                android.R.layout.simple_spinner_dropdown_item));

        this.client = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        this.client.connect();
    }

    public void clickGo(View view)
    {
        if (!this.client.isConnected()) {
            AlertDialog alert = new AlertDialog.Builder(MainActivity.this).create();
            alert.setTitle("Not connected");
            alert.setMessage("Not yet connected to location services");
            alert.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which)
                        { dialog.dismiss(); }
                    });
            alert.show();
            return;
        }

        double radius = Double.parseDouble(((Spinner)findViewById(R.id.spnRadius)).getSelectedItem() + "");
        String type = ((Spinner)findViewById(R.id.spnType)).getSelectedItem().toString().toLowerCase();
        String url = "http://hc.milodavis.com/getLocation.php?"
                + "locType="   + type
                + "&userLat="  + this.latitude
                + "&userLong=" + this.longitude
                + "&radius="   + radius;

        try {
            Intent go = new Intent(this, PlayingActivity.class);
            HttpEntity entity = new DefaultHttpClient().execute(new HttpGet(url)).getEntity();

            if (entity == null)
                return;

            InputStream in = entity.getContent();
            String result = streamToString(in);
            in.close();

            if (result.contains("latitude")) {
                go.putExtra("json", result);
                go.putExtra("userLat",  this.latitude);
                go.putExtra("userLong", this.longitude);
                this.startActivity(go);
            }
            else {
                AlertDialog alert = new AlertDialog.Builder(MainActivity.this).create();
                alert.setTitle("Nothing found");
                alert.setMessage("No locations were found within the specified radius.");
                alert.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which)
                            { dialog.dismiss(); }
                        });
                alert.show();
            }
            //((Button) findViewById(R.id.btnGo)).setText(result);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String streamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        try {
            String line;
            while ((line = reader.readLine()) != null)
                sb.append(line + "\n");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return sb.toString();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return item.getItemId() == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    public void onConnected(Bundle connectionHint) {
        String permission = Manifest.permission.ACCESS_FINE_LOCATION;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[] { permission }, 0);

        Location loc = LocationServices.FusedLocationApi.getLastLocation(this.client);

        if (loc != null) {
            this.latitude  = loc.getLatitude();
            this.longitude = loc.getLongitude();
        }
    }

    public void onConnectionSuspended(int cause) { }
}
