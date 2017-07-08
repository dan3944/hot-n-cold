package idonthaveadomain.hotcold;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class PlayingActivity extends Activity implements ConnectionCallbacks, LocationListener
{
    private TextView lblTemp;
    private RelativeLayout layout;
    private GoogleApiClient client;
    private LocationRequest locRequest;
    private double initDist, distance;
    private double prevProgress, progress;
    private double goalLat, goalLong;
    private String goalName;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_playing);
        this.lblTemp = (TextView) findViewById(R.id.lblTemperature);
        this.lblTemp.setText("");
        this.layout = (RelativeLayout)findViewById(R.id.layout);
        this.layout.setBackgroundColor(Color.BLUE);
        this.locRequest = new LocationRequest();
        this.locRequest.setInterval(5000);
        this.locRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        this.client = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        this.client.connect();

        Intent i = getIntent();
        String json = i.getStringExtra("json");

        int latI  = json.indexOf("latitude")  + "latitude:\"".length();
        int longI = json.indexOf("longitude") + "longitude:\"".length();
        int nameI = json.indexOf("name")      + "name:\"".length() + 1;
        this.goalLat  = Double.parseDouble(json.substring(latI,  json.indexOf("longitude") - 2));
        this.goalLong = Double.parseDouble(json.substring(longI, json.indexOf("name")      - 2));
        this.goalName = json.substring(nameI, json.indexOf("link") - 3);

        float[] dist = new float[1];
        Location.distanceBetween(i.getDoubleExtra("userLat", 0), i.getDoubleExtra("userLong", 0), this.goalLat, this.goalLong, dist);
        this.initDist = this.distance = dist[0];
        this.progress = this.prevProgress = 0;
        //lblTemp.setText(goalLat + ", " + goalLong + "\n" + goalName);
    }

    public void onLocationChanged(Location loc) {
        float[] results = new float[1];

        if (loc != null) {
            Location.distanceBetween(loc.getLatitude(), loc.getLongitude(), this.goalLat, this.goalLong, results);
            this.distance = results[0];

            this.prevProgress = this.progress;
            this.progress = Math.max(1 - this.distance / this.initDist, 0);
        }

        this.updateUI();
    }

    public void updateUI() {
        int startRed   = 0;
        int startGreen = 0;
        int startBlue  = 255;

        int midRed     = 255;
        int midGreen   = 255;
        int midBlue    = 255;

        int finalRed   = 255;
        int finalGreen = 0;
        int finalBlue  = 0;

        int r, g, b;

        if (this.distance < 15)
            this.lblTemp.setText("You have arrived:\n" + this.goalName);
        else if (this.prevProgress < this.progress)
            this.lblTemp.setText("Warmer");
        else if (this.prevProgress > this.progress)
            this.lblTemp.setText("Colder");
        else
            this.lblTemp.setText("");

        if (this.progress < 0.5) {
            r = (int) (2 * (midRed   * progress + startRed   * (0.5 - progress)));
            g = (int) (2 * (midGreen * progress + startGreen * (0.5 - progress)));
            b = (int) (2 * (midBlue  * progress + startBlue  * (0.5 - progress)));
        }
        else {
            r = (int) (2 * (finalRed   * (progress - 0.5) + midRed   * (1 - progress)));
            g = (int) (2 * (finalGreen * (progress - 0.5) + midGreen * (1 - progress)));
            b = (int) (2 * (finalBlue  * (progress - 0.5) + midBlue  * (1 - progress)));
        }
        //r = (r << 16) & 0x00FF0000;
        //g = (g << 8)  & 0x0000FF00;
        //b = b         & 0x000000FF;
        layout.setBackgroundColor(android.graphics.Color.rgb(r, g, b));//0xFF000000 | r | g | b);
    }

    public void onConnected(Bundle connectionHint) {
        String permission = Manifest.permission.ACCESS_FINE_LOCATION;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[] { permission }, 0);

        LocationServices.FusedLocationApi.requestLocationUpdates(this.client, this.locRequest, this);
    }

    public void onConnectionSuspended(int cause) { }
}