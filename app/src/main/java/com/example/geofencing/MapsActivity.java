package com.example.geofencing;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Toast;

import com.example.geofencing.Interface.IOnLoadLocationListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GeoQueryEventListener, IOnLoadLocationListener {

    private GoogleMap mMap;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Marker currentUser;
    private DatabaseReference myLocationRef;
    private GeoFire geoFire;
    private List<LatLng> illegal;
    private IOnLoadLocationListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {


                        buildLocationRequest();
                        buildLocationCallback();
                        fusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(MapsActivity.this);



                        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
//                        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
//                                .findFragmentById(R.id.map);
//                        mapFragment.getMapAsync(MapsActivity.this);

                        initArea();

                        settingGeoFire();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MapsActivity.this,"You must enable permission",Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();


    }

    private void initArea() {

        listener=this;
        //load from firebase database

        FirebaseDatabase.getInstance().getReference("illegal").child("MyCity").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<MyLatLng> latLngList= new ArrayList<>();
                for(DataSnapshot locationSnapShot: dataSnapshot.getChildren()){
                    MyLatLng latLng= locationSnapShot.getValue(MyLatLng.class);
                    latLngList.add(latLng);
                }
                listener.onLoadLocationSuccess(latLngList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                    listener.onLoadLocationFailed(databaseError.getMessage());
            }
        });
//        illegal= new ArrayList<>();
//        illegal.add(new LatLng(28.68088,77.3155));
//        illegal.add(new LatLng(28.66,77.23));
//        illegal.add(new LatLng(28.77,77.29));


//        FirebaseDatabase.getInstance()
//                .getReference("illegal")
//                .child("MyCity").setValue(illegal).addOnCompleteListener(new OnCompleteListener<Void>() {
//            @Override
//            public void onComplete(@NonNull Task<Void> task) {
//                Toast.makeText(MapsActivity.this,"Updated!",Toast.LENGTH_SHORT).show();
//            }
//        }).addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception e) {
//                Toast.makeText(MapsActivity.this,""+e.getMessage(),Toast.LENGTH_SHORT).show();
//            }
//        });
   }

    private void settingGeoFire() {
        myLocationRef= FirebaseDatabase.getInstance().getReference("MyLocation");
        geoFire= new GeoFire(myLocationRef);
    }

    private void buildLocationCallback() {

        locationCallback= new LocationCallback(){

            @Override
            public void onLocationResult(final LocationResult locationResult){
                if(mMap != null){


                    geoFire.setLocation("You", new GeoLocation(locationResult.getLastLocation().getLatitude(),
                            locationResult.getLastLocation().getLongitude()), new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {

                            if(currentUser != null) currentUser.remove();
                            currentUser=mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(locationResult.getLastLocation().getLatitude(),locationResult.getLastLocation().getLongitude()))
                                    .title("YOU"));

                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentUser.getPosition(),12.0f));

                        }
                    });

                }
            }



        };



    }

    private void buildLocationRequest() {

        locationRequest= new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setSmallestDisplacement(10f);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        if(fusedLocationProviderClient != null)
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
                if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)  != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    return;
            }


            fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper());

            // add circle for illegal
            for(LatLng latLng : illegal){
                mMap.addCircle(new CircleOptions().center(latLng).radius(50).strokeColor(Color.BLUE).fillColor(0x220000FF).strokeWidth(5.0f));

                //geoQuery for user in illegal area
                GeoQuery geoQuery= geoFire.queryAtLocation(new GeoLocation(latLng.latitude,latLng.longitude),0.05f);
                geoQuery.addGeoQueryEventListener(MapsActivity.this);
            }

    }

    @Override
    protected void onStop(){
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        super.onStop();
    }

    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        sendNotification("Alert", String.format("%s are near a Pothole, Please go slow!!", key));
    }



    @Override
    public void onKeyExited(String key) {
        sendNotification("Alert", String.format("%s are in Safe Area!!", key));
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
       // sendNotification("Alert", String.format("%s please move with slow speed!!", key));
    }

    @Override
    public void onGeoQueryReady() {

    }

    @Override
    public void onGeoQueryError(DatabaseError error) {

        Toast.makeText(this,""+error.getMessage(),Toast.LENGTH_SHORT).show();

    }


    private void sendNotification(String title, String content) {
        String NOTIFICATION_CHANNEL_ID= "channelId";
        NotificationManager notificationManager= (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel notificationChannel= new NotificationChannel(NOTIFICATION_CHANNEL_ID,"My Notification", NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setDescription("Channel Description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0,1000,500,1000});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        NotificationCompat.Builder builder= new NotificationCompat.Builder(this,NOTIFICATION_CHANNEL_ID);
        builder.setContentTitle(title).setContentText(content).setAutoCancel(false).setSmallIcon(R.mipmap.ic_launcher).setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));

        Notification notification= builder.build();
        notificationManager.notify(new Random().nextInt(),notification);

    }

    @Override
    public void onLoadLocationSuccess(List<MyLatLng> latLngs) {
        illegal= new ArrayList<>();
        for(MyLatLng myLatLng : latLngs){
            LatLng convert= new LatLng(myLatLng.getLatitude(),myLatLng.getLongitude());
            illegal.add(convert);
        }

        // call to map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapsActivity.this);
    }

    @Override
    public void onLoadLocationFailed(String message) {
        Toast.makeText(this,""+message,Toast.LENGTH_SHORT).show();
    }
}
