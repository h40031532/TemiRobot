package tw.com.temirobot;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.robotemi.sdk.Robot;
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener;
import com.robotemi.sdk.listeners.OnRobotReadyListener;
import com.robotemi.sdk.navigation.listener.OnCurrentPositionChangedListener;
import com.robotemi.sdk.navigation.model.Position;
import com.robotemi.sdk.navigation.model.SpeedLevel;

import org.jetbrains.annotations.NotNull;
import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements
        OnGoToLocationStatusChangedListener,
        OnCurrentPositionChangedListener,
        OnRobotReadyListener {
    private static final String LOG_TAG = "MainActivity";
    private static final String TAG = "MediaRecorderUtil";

    private static Robot robot;
    private static FirebaseStorage storage;
    private StorageReference mStorageRef;
    private DatabaseReference mDatabase;
    private static final String TAG_f = "Firebase";

    //????????????????????????
//    private final String FileName = getExternalFilesDir("").getAbsolutePath();
    //??????????????????
//    private MediaPlayer mPlayer = null;

    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
    private String audioname = "";
    private String placename = "";
    //??????????????????
    private MediaRecorder recorder;

    private MainActivity.Type type1 = MainActivity.Type.AAC_AAC;
    private MainActivity.Type type2 = MainActivity.Type.AAC_M4A;
    private MainActivity.Type type3 = MainActivity.Type.AMR_AMR;

    public static float dbCount = 40;
    private static float lastDbCount = dbCount;
    private static float min = 0.5f;  //????????????????????????
    private static float value = 0;   // ???????????????

    private int timerval = 0;
    private int y = 0;
    private TimerTask task = null;
    private Timer timer = null;
    private static final long PERIOD_DAY = 24 * 60 * 60 * 1000;
    private static final String TAGError = "Recorder";

    //    //????????????
//    private static final String TAG_fr = "FaceRecognition";
    private static final int PERMISSION_CODE = 1001;
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private PreviewView previewView;
    private CameraSelector cameraSelector;
    private ProcessCameraProvider cameraProvider;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private Preview previewUseCase;
    private ImageAnalysis analysisUseCase;
    private pl.droidsonroids.gif.GifImageView gifImageView;
    private ImageView bgwhite;
    //    private GraphicOverlay graphicOverlay;
//    private ImageView previewImg;
//
//    private final HashMap<String, SimilarityClassifier.Recognition> registered = new HashMap<>(); //saved Faces
//    private Interpreter tfLite;
    private boolean flipX = false;
//    private boolean start = true;
//    private boolean regis = false;
//    private float[][] embeddings;
//
//    private static final float IMAGE_MEAN = 128.0f;
//    private static final float IMAGE_STD = 128.0f;
//    private static final int INPUT_SIZE = 112;
//    private static final int OUTPUT_SIZE = 192;

    private FirebaseDatabase database = FirebaseDatabase.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        robot = Robot.getInstance();

        checkPermission();
        y = 0;

        mStorageRef = FirebaseStorage.getInstance().getReference();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        storage = FirebaseStorage.getInstance();

        previewView = findViewById(R.id.previewView);
        previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);
//        graphicOverlay = findViewById(R.id.graphic_overlay);
//        previewImg = findViewById(R.id.preview_img);

        bgwhite = findViewById(R.id.bgwhite);
        gifImageView = findViewById(R.id.gifImageView);

        DBTime();
    }

    @Override
    protected void onStart() {
        super.onStart();
        robot.addOnCurrentPositionChangedListener(this);
        robot.addOnGoToLocationStatusChangedListener(this);
        robot.addOnRobotReadyListener(this);

//        audioname = dateFormat.format(calendar.getTime());
//        timerval = 1;
//        robot.goTo(place);
//        startrec(audioname);
//        stoprec();
//        startCamera();
//        mDatabase.child("face").child("temi1").child("patrol").child("py").setValue(true);
//        mDatabase.child("face").child("temi1").child("checkin").child("py").setValue(false);
//        mDatabase.child("face").child("temi1").child("regis").child("py").setValue(false);
//        mDatabase.child("face").child("temi1").child("welcome").child("py").setValue(false);
    }

    @Override
    public void onStop() {
        super.onStop();
        robot.removeOnCurrentPositionChangedListener(this);
        robot.removeOnGoToLocationStatusChangedListener(this);
        robot.removeOnRobotReadyListener(this);
//        try {
//            recorder.stop();
//            recorder.release();
//            recorder = null;
//        } catch (RuntimeException e) {
//            Log.e(TAG,e.toString());
////            recorder.reset();
//            recorder.release();
//            recorder = null;
//
//            File file2 = new File(getExternalFilesDir(""),t+".mp4");
//            if (file2.exists())
//                file2.delete();
//            System.out.println("list: file2: " + t);
//        }
//        timerval = 0;
//        if (recorder != null) {
//            recorder.stop();
//            recorder.release();
//            recorder = null;
//            System.out.println("list: ----????????????----");
//        } else Log.d(TAG, "list: recorder is null.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCamera();
//        DatabaseReference myRef2 = mDatabase.child("face").child("temi1").child("patrol").child("py");
//        myRef2.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                // This method is called once with the initial value and again
//                // whenever data at this location is updated.
//                Boolean value2 = dataSnapshot.getValue(Boolean.class);
//                Log.d("TAG", "Value2 is: " + value2);
//                if (value2 == true) {
//                    startCamera();
//                }
//            }
//
//            @Override
//            public void onCancelled(DatabaseError error) {
//                // Failed to read value
//                Log.w("TAG", "Failed to read value.", error.toException());
//            }
//        });
    }

    public void btnface(View v) {
        Intent it = new Intent(MainActivity.this, FaceRecognition.class);
        startActivity(it);
        finish();
    }

    public void btngame(View v) {
        Intent it = new Intent(MainActivity.this, Game.class);
        startActivity(it);
        finish();
    }

    public void btnregis(View v) {
        Intent it = new Intent(MainActivity.this, Regis.class);
        startActivity(it);
        finish();
    }

    public void btnwelcome(View v) {
        Intent it = new Intent(MainActivity.this, Welcome.class);
        startActivity(it);
        finish();
    }

//    public void btnstartplay(View v){
//        mPlayer = new MediaPlayer();
//        try {
//            mPlayer.setDataSource(new File(getExternalFilesDir(""), "record_a.mp4").getAbsolutePath());
//            mPlayer.prepare();
//            mPlayer.start();
//            System.out.println("list: ??????????????????");
//        } catch (IOException e) {
//            Log.e(LOG_TAG, "list: ????????????");
//        }
//    }
//
//    public void btnstopplay(View v){
//        mPlayer.release();
//        mPlayer = null;
//        System.out.println("list: ??????????????????");
//    }

    public void TimerManager(String hrs, String min, String place2) {
        int inthrs = Integer.parseInt(hrs);
        int intmin = Integer.parseInt(min);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, inthrs);
        calendar.set(Calendar.MINUTE, intmin);
        calendar.set(Calendar.SECOND, 0);
        Date date = calendar.getTime(); //??????????????????????????????
        //?????????????????????????????????????????? ??????????????????
        //???????????? ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        if (date.before(new Date())) {
            date = this.addDay(date, 1);
        }
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                mDatabase.child("face").child("temi1").child("patrol").child("py").setValue(true);
                mDatabase.child("face").child("temi1").child("checkin").child("py").setValue(false);
                mDatabase.child("face").child("temi1").child("regis").child("py").setValue(false);
                mDatabase.child("face").child("temi1").child("welcome").child("py").setValue(false);
//                bgwhite.setVisibility(View.VISIBLE);
//                gifImageView.setVisibility(View.VISIBLE);
                y = 1;
                audioname = dateFormat.format(calendar.getTime());
                startrec(audioname);
                robot.goTo(place2);
            }
        };
        //?????????????????????????????????????????????????????????????????????????????????
        timer.schedule(task, date, PERIOD_DAY);
    }

    // ?????????????????????
    public Date addDay(Date date, int num) {
        Calendar startDT = Calendar.getInstance();
        startDT.setTime(date);
        startDT.add(Calendar.DAY_OF_MONTH, num);
        return startDT.getTime();
    }

    public void DBTime() {
        List<String> patrolid = new ArrayList<>();
        patrolid.add("11");
        patrolid.add("12");
        patrolid.add("13");
        patrolid.add("21");
        patrolid.add("22");
        patrolid.add("23");
        patrolid.add("31");
        patrolid.add("32");
        patrolid.add("33");
        patrolid.add("41");
        patrolid.add("42");
        patrolid.add("43");
        System.out.println(patrolid);
        String strpatrol2 = "";
        for (String strpatrol : patrolid) {
            strpatrol2 = strpatrol;
            DatabaseReference hrsRef = database.getReference("/temi1/" + strpatrol2 + "/hrs");
            String finalStrpatrol = strpatrol2;
            hrsRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // This method is called once with the initial value and again
                    // whenever data at this location is updated.
                    String hrs = dataSnapshot.getValue(String.class);
                    Log.d("TAG", "hrs: " + hrs);

                    DatabaseReference minRef = database.getReference("/temi1/" + finalStrpatrol + "/min");
                    minRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            // This method is called once with the initial value and again
                            // whenever data at this location is updated.
                            String min = dataSnapshot.getValue(String.class);
                            Log.d("TAG", "min: " + min);

                            DatabaseReference placeRef = database.getReference("/temi1/" + finalStrpatrol + "/place");
                            placeRef.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    // This method is called once with the initial value and again
                                    // whenever data at this location is updated.
                                    String place = dataSnapshot.getValue(String.class);
                                    Log.d("TAG", "place: " + place);
                                    if (hrs.trim().length() > 0 && min.trim().length() > 0 && place.trim().length() > 0) {
                                        TimerManager(hrs, min, place);
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError error) {
                                    // Failed to read value
                                    Log.w("TAG", "Failed to read value.", error.toException());
                                }
                            });
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            // Failed to read value
                            Log.w("TAG", "Failed to read value.", error.toException());
                        }
                    });
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Failed to read value
                    Log.w("TAG", "Failed to read value.", error.toException());
                }
            });
        }

    }

    @Override
    public void onRobotReady(boolean isReady) {
        if (isReady) {
            try {
                final ActivityInfo activityInfo = getPackageManager().getActivityInfo(getComponentName(), PackageManager.GET_META_DATA);
                robot.onStart(activityInfo);
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onCurrentPositionChanged(Position position) {
        System.out.println("list:onCurrentPosition Position: " + position.toString());
    }

    @Override
    public void onGoToLocationStatusChanged(@NotNull String location, String status, int descriptionId, @NotNull String description) {
        System.out.println("list: OnGoToLocationStatusChanged");
        switch (status) {
            case OnGoToLocationStatusChangedListener.START:
                try {
                    robot.tiltAngle(0);
                    robot.setGoToSpeed(SpeedLevel.SLOW);
//                    startrec();
                    System.out.println("list: OnGoToLocationStatusChangedListener_START");
                } catch (Exception e) {
                    Log.e(TAGError, "list:Error:" + e.getMessage());
                }
                break;
            case OnGoToLocationStatusChangedListener.GOING:
                try {
                    robot.tiltAngle(0);
                    robot.setGoToSpeed(SpeedLevel.SLOW);
                    System.out.println("list: OnGoToLocationStatusChangedListener_GOING");
                } catch (Exception e) {
                    Log.e(TAGError, "list:Error:" + e.getMessage());
                }
                break;
            case OnGoToLocationStatusChangedListener.CALCULATING:
                robot.tiltAngle(0);
                System.out.println("list: OnGoToLocationStatusChangedListener_CALCULATING");
                //??????
                break;
            case OnGoToLocationStatusChangedListener.COMPLETE:
                try {
                    robot.tiltAngle(55);
                    //robot.repose();
                    //robot.stopMovement();
                    y = 1;
                    stoprec();
                    Thread.sleep(2000);
//                    mDatabase.child("face").child("temi1").child("patrol").child("py").setValue(false);
//                    bgwhite.setVisibility(View.INVISIBLE);
//                    gifImageView.setVisibility(View.INVISIBLE);
                    System.out.println("list: OnGoToLocationStatusChangedListener_COMPLETE");
                } catch (Exception e) {
                    Log.e(TAGError, "list: Error:" + e.getMessage());
                }
                break;
            case OnGoToLocationStatusChangedListener.ABORT:
                robot.tiltAngle(55);
                System.out.println("list: OnGoToLocationStatusChangedListener_ABORT");
                //robot.stopMovement();
                break;
        }
    }

//    private void hideKeyboard(Activity activity) {
//        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
//        //Find the currently focused view, so we can grab the correct window token from it.
//        View view = activity.getCurrentFocus();
//        //If no view currently has focus, create a new one, just so we can grab a window token from it
//        if (view == null) {
//            view = new View(activity);
//        }
//        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
//    }

    /**
     * ??????????????????
     */
    private void checkPermission() {
        System.out.println("list:3 checkPermission");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, permissions, 200);
                    return;
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[]
            grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && requestCode == 200) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivityForResult(intent, 200);
                    return;
                }
            }
        }
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        if (requestCode == PERMISSION_CODE) {
            setupCamera();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 200) {
            checkPermission();
        }
    }


    /**
     * ??????????????????????????????
     */
    public enum Type {
        AAC_M4A(".m4a", MediaRecorder.AudioEncoder.AAC, MediaRecorder.OutputFormat.MPEG_4),
        AAC_AAC(".aac", MediaRecorder.AudioEncoder.AAC, MediaRecorder.OutputFormat.AAC_ADTS),
        AMR_AMR(".amr", MediaRecorder.AudioEncoder.AMR_NB, MediaRecorder.OutputFormat.AMR_NB);
        String ext;
        int audioEncoder;
        int outputFormat;

        Type(String ext, int audioEncoder, int outputFormat) {
            this.ext = ext;
            this.audioEncoder = audioEncoder;
            this.outputFormat = outputFormat;
        }
    }

    /**
     * ??????????????????????????????????????????
     */
//    public void getDB(){
//        System.out.println("list:????????????, getDB");
//        try {
//                if (recorder != null) {
//                    recorder.reset();
//                    System.out.println("list: ----????????????----");
//                }
//                else Log.d(TAG,"list: recorder is null.");
//            final int[] conti = {0};
//            recorder = new MediaRecorder();
//            System.out.println("list: getDB ????????????1");
//            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//            recorder.setOutputFormat(type1.outputFormat);
//            recorder.setAudioEncoder(type1.audioEncoder);
//            //???????????????????????????
//            recorder.setOutputFile(new File(getExternalFilesDir(""),"record_b.mp4")
//                    .getAbsolutePath());
//            System.out.println("list: ??????????????????:"+getExternalFilesDir("").getAbsolutePath()+"/record_b.mp4");
//            recorder.prepare();
//            recorder.start();
//            timer = new Timer();
//            task = new TimerTask() { //???????????????????????????run()???????????????value??????
//                @Override //???value?????????????????????????????????????????????Handler???message???
//                public void run() {
//                    if (timerval == 1) {
//                        value2 = recorder.getMaxAmplitude();
//                        System.out.println("list: ?????????: " + value2);
//                        if (value2 > 0 && value2 < 1000000) {
//                            setDbCount(20 * (float) (Math.log10(value2)));  //???????????????????????????
//                            System.out.println("list: ?????????: " + lastDbCount);
//                        } else System.out.println("list: ?????????????????????");
//                        switch(conti[0]) {
//                            case 0:
//                                if (lastDbCount >= 55 ){
//                                    startrec();
//                                    conti[0]++;
//                                }
//                                break;
//                            case 1:
//                                if (lastDbCount < 55){
//                                    stoprec();
//                                    try {
//                                        if (recorder != null) {
//                                            recorder.reset();
//                                            System.out.println("list: ----????????????----");
//                                        }
//                                        else System.out.println("list: recorder is null.");
//                                        recorder = new MediaRecorder();
//                                        System.out.println("list: stoprec ????????????1");
//                                        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//                                        recorder.setOutputFormat(type1.outputFormat);
//                                        recorder.setAudioEncoder(type1.audioEncoder);
//                                        //???????????????????????????
//                                        recorder.setOutputFile(new File(getExternalFilesDir(""),"record_b.mp4")
//                                                .getAbsolutePath());
//                                        System.out.println("list: ??????????????????:"+getExternalFilesDir("").getAbsolutePath()+"/record_b.mp4");
//                                        recorder.prepare();
//                                        recorder.start();
//                                        System.out.println("list: stoprec ????????????2");
//                                    } catch (IOException e) {
//                                        e.printStackTrace();
//                                        Log.d(TAG,"list: stoprec ??????????????????");
//                                    }
//                                    conti[0]--;
//                                }
//                                break;
//                        }
//                    }
//                    else {
//                        //Log.d(TAG,"list: ??????????????????");
//                    }
//                }
//            };
//            timer.schedule(task, 100,2000); //timer????????????100???????????????task??????(???????????????task???start()??????)
//            //timer????????????????????????????????????????????????????????????????????????????????????getMaxAmplitude()???????????????????????????????????????(??????????????????????????????)
//        } catch (IOException e) {
//            e.printStackTrace();
//            Log.d(LOG_TAG, String.valueOf(e));
//            Log.d(TAG,"list: ??????????????????");
//        }
//    }
//
//    public static float setDbCount(float dbValue) {
//        if (dbValue > lastDbCount) {
//            value = dbValue - lastDbCount > min ? dbValue - lastDbCount : min;
//        }else{
//            value = dbValue - lastDbCount < -min ? dbValue - lastDbCount : -min;
//        }
//        dbCount = lastDbCount + value * 0.2f ; //????????????????????????
//        lastDbCount = dbCount;
//        return lastDbCount;
//    }
    public void startrec(String audioname) {
        System.out.println("list:3 startrec t: " + audioname);
//        try {
//            if (recorder != null) {
//                recorder.reset();
//                System.out.println("list: ----????????????----");
//            }
//            else System.out.println("list: recorder is null.");
//            recorder = new MediaRecorder();
//            System.out.println("list: startrec ????????????1");
//            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//            recorder.setOutputFormat(type1.outputFormat);
//            recorder.setAudioEncoder(type1.audioEncoder);
//            //???????????????????????????
//            recorder.setOutputFile(new File(getExternalFilesDir(""),"record_a.mp4")
//                    .getAbsolutePath());
//            System.out.println("list: ??????????????????:"+getExternalFilesDir("").getAbsolutePath()+"/record_a.mp4");
//            recorder.prepare();
//            recorder.start();
//            System.out.println("list: startrec ????????????2");
//        } catch (IOException e) {
//            e.printStackTrace();
//            Log.d(TAG,"list: startrec ??????????????????");
//        }

        // ????????????
        /* ???Initial????????????MediaRecorder?????? */
        if (recorder == null) {
            recorder = new MediaRecorder();
            System.out.println("list:3 ????????????: " + recorder);
        } else {
            System.out.println("list:3 ????????????(nonnull): " + recorder);
        }
        try {
            /* ???setAudioSource/setVedioSource */
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);// ???????????????
            /*
             * ?????????????????????????????????THREE_GPP/MPEG-4/RAW_AMR/Default THREE_GPP(3gp??????
             * ???H263??????/ARM????????????)???MPEG-4???RAW_AMR(???????????????????????????????????????AMR_NB)
             */
//            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setOutputFormat(type1.outputFormat);
            /* ?????????????????????????????????AAC/AMR_NB/AMR_MB/Default ?????????????????????????????? */
//            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncoder(type1.audioEncoder);
            /* ????????? */
            recorder.setOutputFile(new File(getExternalFilesDir(""), audioname + ".mp4").getAbsolutePath());
            recorder.prepare();
            /* ????????? */
            recorder.start();
        } catch (IllegalStateException e) {
            Log.i(TAG, "call startAmr(File mRecAudioFile) failed!" + e.getMessage());
        } catch (IOException e) {
            Log.i(TAG, "call startAmr(File mRecAudioFile) failed!" + e.getMessage());
        }
    }

    public void stoprec() {
        try {
            System.out.println("list:3 stoprec ????????????: " + recorder);
            recorder.stop();
            recorder.release();
            recorder = null;
            uploadAudio(audioname);
        } catch (RuntimeException e) {
            Log.e(TAG, e.toString());
            System.out.println("list:3 stoprec ???????????? e: " + recorder);
            uploadAudio(audioname);
//            recorder.reset();
//            recorder.release();
            recorder = null;
//            File file3 = new File(getExternalFilesDir(""), audioname + ".mp4");
//            if (file3.exists())
//                file3.delete();
//            System.out.println("list:3 stoprec file3 delete: " + audioname);
        }
    }


    public void uploadAudio(String audioname2) {
        // Create a storage reference from our app
        StorageReference storageRef = storage.getReference();

        // Create a reference to "record_a.mp4"
        StorageReference recordRef = storageRef.child(audioname2 + ".mp4");

        // Create a reference to 'audios/record_a.mp4'
        StorageReference recordAudiosRef = storageRef.child("audios/" + audioname2 + ".mp4");

        // While the file names are the same, the references point to different files
        recordRef.getName().equals(recordAudiosRef.getName());    // true
        recordRef.getPath().equals(recordAudiosRef.getPath());    // false

        // Create file metadata including the content type
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("audio/mpeg")
                .build();

//        Uri file = Uri.fromFile(new File("path/to/record_a.mp4"));
        Uri file = Uri.fromFile(new File(getExternalFilesDir(""), audioname2 + ".mp4"));
        recordRef = storageRef.child("audios/" + file.getLastPathSegment());
        // Upload the file and metadata
        //UploadTask uploadTask = storageRef.child("audios/record_a.mpeg").putFile(file, metadata);
        UploadTask uploadTask = recordRef.putFile(file, metadata);

        // Observe state change events such as progress, pause, and resume
        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress3 = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                Log.d(TAG_f, "list:3 UploadAudio is " + progress3 + "% done");
                if (progress3 >= 100.0) {
                    File file = new File(getExternalFilesDir(""), audioname2 + ".mp4");
//                    if (file.exists())
//                        file.delete();
                    System.out.println("list:3 t uploadAudio: " + audioname2);
                    mDatabase.child("face").child("temi1").child("patrol").child("py").setValue(false);
                }
            }
        }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG_f, "list: Upload is paused");
            }
        });

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                int errorCode = ((StorageException) exception).getErrorCode();
                String errorMessage = exception.getMessage();
                // test the errorCode and errorMessage, and handle accordingly
                Log.d(TAG_f, "list: upload failure");
                Log.d(TAG_f, "list: errorCode: " + errorCode + ", errorMessage: " + errorMessage);
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                System.out.println("list: upload task: " + taskSnapshot.toString());
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // If there's an upload in progress, save the reference so you can query it later
        if (mStorageRef != null) {
            outState.putString("reference", mStorageRef.toString());
            Log.d(TAG_f, "list: outstate: " + mStorageRef.toString());
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // If there was an upload in progress, get its reference and create a new StorageReference
        final String stringRef = savedInstanceState.getString("reference");
        if (stringRef == null) {
            return;
        }
        mStorageRef = FirebaseStorage.getInstance().getReferenceFromUrl(stringRef);

        // Find all UploadTasks under this StorageReference (in this example, there should be one)
        List<UploadTask> tasks = mStorageRef.getActiveUploadTasks();
        if (tasks.size() > 0) {
            // Get the task monitoring the upload
            UploadTask task = tasks.get(0);

            // Add new listeners to the task using an Activity scope
            task.addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot state) {
                    // Success!
                    Log.d(TAG_f, "list: upload state: " + state.toString());
                }
            });
        }

        // Find all DownloadTasks under this StorageReference (in this example, there should be one)
        List<FileDownloadTask> tasks2 = mStorageRef.getActiveDownloadTasks();
        if (tasks.size() > 0) {
            // Get the task monitoring the download
            FileDownloadTask task = tasks2.get(0);

            // Add new listeners to the task using an Activity scope
            task.addOnSuccessListener(this, new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot state) {
                    // Success!
                    Log.d(TAG_f, "list: Instance Success: " + state);
                }
            });
        }
    }


    /** ???????????? */
    /**
     * Permissions Handler
     */
    private void getPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{CAMERA_PERMISSION}, PERMISSION_CODE);
    }

    /**
     * Setup camera & use cases
     */
    private void startCamera() {
        if (ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            setupCamera();
            System.out.println("list:2 startCamera1");
        } else {
            getPermissions();
            System.out.println("list:2 startCamera2");
        }
    }

    private void setupCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);
        System.out.println("list:2 setupCamera");

        cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindAllCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "cameraProviderFuture.addListener Error", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindAllCameraUseCases() {
        System.out.println("list:2 bindAllCameraUseCases");

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            bindPreviewUseCase();
            bindAnalysisUseCase();
        }
    }

    private void bindPreviewUseCase() {
        System.out.println("list:2 bindPreviewUseCase");

        if (cameraProvider == null) {
            return;
        }

        if (previewUseCase != null) {
            cameraProvider.unbind(previewUseCase);
        }

        Preview.Builder builder = new Preview.Builder();
        builder.setTargetAspectRatio(AspectRatio.RATIO_4_3);
        //builder.setTargetRotation(getRotation());

        previewUseCase = builder.build();
        previewUseCase.setSurfaceProvider(previewView.getSurfaceProvider());

        try {
            cameraProvider
                    .bindToLifecycle(this, cameraSelector, previewUseCase);
        } catch (Exception e) {
            Log.e(TAG, "Error when bind preview", e);
        }
    }

    private void bindAnalysisUseCase() {
        System.out.println("list:2 bindAnalysisUseCase");

        if (cameraProvider == null) {
            return;
        }

        if (analysisUseCase != null) {
            cameraProvider.unbind(analysisUseCase);
        }

        Executor cameraExecutor = Executors.newSingleThreadExecutor();

        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        builder.setTargetAspectRatio(AspectRatio.RATIO_4_3);
//        builder.setTargetRotation(getRotation());

        analysisUseCase = builder.build();
        analysisUseCase.setAnalyzer(cameraExecutor, this::analyze);

        try {
            cameraProvider
                    .bindToLifecycle(this, cameraSelector, analysisUseCase);
        } catch (Exception e) {
            Log.e(TAG, "Error when bind analysis", e);
        }
    }

//    protected int getRotation() {
////            throws NullPointerException {
//        System.out.println("list:2 getRotation");
//
//        return previewView.getDisplay().getRotation();
//    }

//    public InputImage downloadImage(InputImage inputImage2, StorageReference pathReference, File localFile) {
//        System.out.println("list:2 downloadImage");
//
////        FirebaseOptions opts = FirebaseApp.getInstance().getOptions();
////        Log.d(TAG_f, "list: Bucket = " + opts.getStorageBucket());
//        //list: Bucket = temirobot-1.appspot.com
//        ///b/temirobot-1.appspot.com/o/images
//
////        // Create a storage reference from our app
////        StorageReference storageRef = storage.getReference();
//
//        pathReference.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
//            @Override
//            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
//                Log.d(TAG_f,"list:2 download: " + taskSnapshot);
//                // Local temp file has been created
//            }
//        }).addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception exception) {
//                Log.d(TAG_f,"list:2 exception" + exception);
//                // Handle any errors
//            }
//        });
//        if (localFile.exists()) {
//            try {
//                Uri newUrl2 = Uri.fromFile(localFile);
//                String newUrl3 = newUrl2.toString();
//                Log.i(TAG_f, "list:2 newUrl3: " + newUrl3);
//                inputImage2 = InputImage.fromFilePath(getApplicationContext(), newUrl2);
//            } catch (IOException e) {
//                Log.d(TAG, String.valueOf(e));
//                System.out.println("list:2 input: " + e);
//            }
//        }
////        if (inputImage2 !=null){
////            System.out.println("list:2 inputImage1 success");
////        }else System.out.println("list:2 inputImage1 null");
//
//        return inputImage2;
//    }
//
//    /** Face detection processor */
//    @SuppressLint("UnsafeOptInUsageError")
//    private void regisAnalyze() {
//        System.out.println("list:2 regisAnalyze");
//        int i = 1;
//        String[] matrix = new String[100];
//        File localFile = null;
//        StorageReference storageRef = storage.getReference();
//        StorageReference pathReference = null;
//        // Create a storage reference from our app
//
//        matrix[1] = "????????? ??????, B0844230";
//        matrix[2] = "???????????????, B0844230";
//        matrix[3] = "????????? ??????, B0844132";
//        matrix[4] = "???????????????, B0844132";
//        matrix[5] = "????????? ??????, B0844227";
//        matrix[6] = "???????????????, B0844227";
//        matrix[7] = "????????? ??????, B0844138";
//        matrix[8] = "???????????????, B0844138";
//        matrix[9] = "????????? ??????, B0844219";
//        matrix[10] = "???????????????, B0844219";
//        matrix[11] = "????????? ??????, 11";
//        matrix[12] = "????????? ??????, 12";
//        matrix[13] = "????????? ??????, 13";
//        matrix[14] = "?????? ??????, 14";
//
//        for(i = 1; i <= 14; i++) {
//            String i2 = Integer.toString(i);
//            String fileName = "/" + i2 + ".jpg";
////            System.out.println("list:3 i:" + i);
//            // Create a reference with an initial file path and name
//            pathReference = storageRef.child("images").child(fileName);
//            localFile = new File(getExternalFilesDir("").getAbsolutePath() + fileName);
//            final InputImage[] inputImage2 = new InputImage[103];
////            System.out.println("list:3 i:" + i);
//            System.out.println("list:2 i:" + inputImage2[i] +",path: "+ pathReference +", localFile: "+ localFile);
//            inputImage2[i] = downloadImage(inputImage2[i], pathReference, localFile);
//            if (inputImage2[i] != null) {
//                System.out.println("list:2 inputImage: success");
//                FaceDetector faceDetector = FaceDetection.getClient();
//
//                InputImage finalInputImage2 = inputImage2[i];
//
//                File finalLocalFile = localFile;
//                faceDetector.process(finalInputImage2)
//                            .addOnSuccessListener(faces -> onSuccessListener(faces, finalInputImage2, finalLocalFile))
//                            .addOnFailureListener(e -> Log.e(TAG, "Barcode process failure", e));
////                .addOnCompleteListener(task -> image2.close());
//
//                System.out.println("list:2 embedding1-0: " + embeddings);
//                String input = matrix[i];
//                embeddings = new float[1][OUTPUT_SIZE]; //output of model will be stored in this variable
//                start = false;
//                SimilarityClassifier.Recognition result = new SimilarityClassifier.Recognition(
//                        "0", "", -1f);
//                System.out.println("list:2 result: " + result);
//                System.out.println("list:2 embedding1-1: " + embeddings);
//                result.setExtra(embeddings);
//                System.out.println("list:2 embedding1-2: " + embeddings);
//                registered.put(input, result);
//                System.out.println("list:2 registered name: " + input);
//                System.out.println("list:2 registered1: " + registered);
//                start = true;
//                } else System.out.println("list:2 inputImage null");
//        }
//    }

    @SuppressLint("UnsafeOptInUsageError")
    private void analyze(@NonNull ImageProxy image) {
        System.out.println("list:2 analyze");
        mDatabase.child("face").child("temi1").child("patrol").child("id").setValue("");

        if (image.getImage() == null) return;

        InputImage inputImage = InputImage.fromMediaImage(
                image.getImage(),
                image.getImageInfo().getRotationDegrees()
        );

        FaceDetector faceDetector = FaceDetection.getClient();

        faceDetector.process(inputImage)
                .addOnSuccessListener(faces -> onSuccessListener(faces, inputImage))
                .addOnFailureListener(e -> Log.e(TAG, "Barcode process failure", e))
                .addOnCompleteListener(task -> image.close());
    }

    private void onSuccessListener(List<Face> faces, InputImage inputImage) {
        System.out.println("list:2 onSuccessListener");
        Rect boundingBox = null;
        //String name = null;
        //float scaleX = (float) previewView.getWidth() / (float) inputImage.getHeight();
        //float scaleY = (float) previewView.getHeight() / (float) inputImage.getWidth();
        if (faces.size() > 0) {

//            // get first face detected
//            Face face = faces.get(0);
//
//            // get bounding box of face;
//            boundingBox = face.getBoundingBox();
//
            // convert img to bitmap & crop img
            Bitmap bitmapImage = mediaImgToBmp(
                    inputImage,
                    inputImage.getRotationDegrees(),
                    boundingBox);
//            System.out.println("list:2 onSuccessListener4: " + inputImage.getMediaImage());
//            System.out.println("list:2 bitmap4: " + bitmap);

            if (y == 1) {
                uploadImage2(bitmapImage);
            }
            if (y >= 1) {
                y++;
                uploadImage(bitmapImage);
            }
        }
    }

    public void uploadImage(Bitmap bitmap) {
        Log.d(TAG_f, "list:3 uploadImage1");
        // Create a storage reference from our app
        StorageReference storageRef = storage.getReference();

        StorageReference checkinRef = storageRef.child("images").child("unknown").child("unknown1.jpg");

//        UploadTask uploadTask = checkinRef.putFile(file);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();
        UploadTask uploadTask = checkinRef.putBytes(data);

        // Observe state change events such as progress, pause, and resume
        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                Log.d(TAG_f, "list:3 Upload1 is " + progress + "% done");
            }
        }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG_f, "list:3 Upload1 is paused");
            }
        });

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                // ...
            }
        });
    }


    public void uploadImage2(Bitmap bitmap) {
        Log.d(TAG_f, "list:3 uploadImage2");
        // Create a storage reference from our app
        StorageReference storageRef = storage.getReference();

        StorageReference checkinRef = storageRef.child("images").child("patrol").child(audioname + ".jpg");

//        UploadTask uploadTask = checkinRef.putFile(file);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();
        UploadTask uploadTask = checkinRef.putBytes(data);

        // Observe state change events such as progress, pause, and resume
        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                Log.d(TAG_f, "list:3 Upload2 is " + progress + "% done");
            }
        }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG_f, "list:3 Upload2 is paused");
            }
        });

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                // ...
            }
        });
    }

    /** Recognize Processor */
//    public String recognizeImage(final Bitmap bitmap, boolean regis) {
//        System.out.println("list:2 recognizeImage");
//        // set image to preview
//        previewImg.setImageBitmap(bitmap);
//
//        //Create ByteBuffer to store normalized image
//        ByteBuffer imgData = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * 3 * 4);
//        imgData.order(ByteOrder.nativeOrder());
//        int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
//
//        System.out.println("list:2 imgData: " + imgData);
//        //get pixel values from Bitmap to normalize
//        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
//        System.out.println("list:2 getPixels: intValues = " + intValues + ", offset = 0" + "bitmap.getWidth = " + bitmap.getWidth() + ", x = 0, y = 0, bitmap.getWidth = " + bitmap.getWidth() + "bitmap.getHeight = " + bitmap.getHeight());
//        System.out.println("list:2 imgData.rewind: " + imgData);
//        imgData.rewind();
//
//        for (int i = 0; i < INPUT_SIZE; ++i) {
//            for (int j = 0; j < INPUT_SIZE; ++j) {
//                int pixelValue = intValues[i * INPUT_SIZE + j];
//                imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
//                imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
//                imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
//            }
//        }
//        System.out.println("list:2 imgData.putfloat: " + imgData);
//        //imgData is input to our model
//        Object[] inputArray = {imgData};
//        System.out.println("list:2 inputArray: " + inputArray);
//        Map<Integer, Object> outputMap = new HashMap<>();
//        System.out.println("list:2 embedding3-0: "+embeddings);
//        embeddings = new float[1][OUTPUT_SIZE]; //output of model will be stored in this variable
//        System.out.println("list:2 embedding3-1: "+embeddings);
//        outputMap.put(0, embeddings);
//        System.out.println("list:2 embedding3-2: "+embeddings);
//        System.out.println("list:2 outputMap: " + outputMap);
//        tfLite.runForMultipleInputsOutputs(inputArray, outputMap); //Run model
//
//        float distance;
//        //Compare new face with saved Faces.
//        if (!regis){
//            if (registered.size() > 0 ) {
//                final Pair<String, Float> nearest = findNearest(embeddings[0]);//Find closest matching face
//                System.out.println("list:2 findNearest embeddings[0]: " + embeddings[0]);
//                System.out.println("list:2 findNearest embeddings: " + embeddings);
//                System.out.println("list:2 Nearest: " + nearest.first);
//                if (nearest != null) {
//                    final String name = nearest.first;
//                    distance = nearest.second;
//                    if (distance < 1.000f) //If distance between Closest found face is more than 1.000 ,then output UNKNOWN face.
//                        return name;
////                    else
////                        return "unknown";
//                }
//            }
//        }
//        return null;
//    }

//    //Compare Faces by distance between face embeddings
//    private Pair<String, Float> findNearest(float[] emb) {
//        System.out.println("list:2 findNearest");
//        Pair<String, Float> ret = null;
//        for (Map.Entry<String, SimilarityClassifier.Recognition> entry : registered.entrySet()) {
//            final String name = entry.getKey();
//            final float[] knownEmb = ((float[][]) entry.getValue().getExtra())[0];
//            float distance = 0;
//            for (int i = 0; i < emb.length; i++) {
//                float diff = emb[i] - knownEmb[i];
//                distance += diff*diff;
//            }
//            distance = (float) Math.sqrt(distance);
//            if (ret == null || distance < ret.second) {
//                ret = new Pair<>(name, distance);
//            }
//        }
//        System.out.println("list:2 findNearest ret: "+ ret);
//        return ret;
//    }


    /** Recognize Processor */
    /**
     * Bitmap Converter
     *
     * @return
     */
    private Bitmap mediaImgToBmp(InputImage image2, int rotation, Rect boundingBox) {
        System.out.println("list:2 mediaImgToBmp");
        System.out.println("list:2 mediaImgToBmp image: " + image2);
        Bitmap frame_bmp1 = null;

        Image image = image2.getMediaImage();
        //Convert media image to Bitmap
        Bitmap frame_bmp = toBitmap(image);
        //Adjust orientation of Face
        frame_bmp1 = rotateBitmap(frame_bmp, rotation, flipX);

        return frame_bmp1;
    }

    private static Bitmap rotateBitmap(Bitmap bitmap, int rotationDegrees, boolean flipX) {
        System.out.println("list:2 rotateBitmap");

        Matrix matrix = new Matrix();

        // Rotate the image back to straight.
        matrix.postRotate(rotationDegrees);

        // Mirror the image along the X or Y axis.
        matrix.postScale(flipX ? -1.0f : 1.0f, 1.0f);
        Bitmap rotatedBitmap =
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        // Recycle the old bitmap if it has changed.
        if (rotatedBitmap != bitmap) {
            bitmap.recycle();
        }
        return rotatedBitmap;
    }

    private static byte[] YUV_420_888toNV21(Image image) {
        System.out.println("list:2 YUV_420_888toNV21");

        int width = image.getWidth();//640
        int height = image.getHeight();//480
        int ySize = width * height;
        int uvSize = width * height / 4;

        byte[] nv21 = new byte[ySize + uvSize * 2];

        //1,2,2
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer(); // Y
        //list: yBuffer: java.nio.DirectByteBuffer[pos=0 lim=307200 cap=307200]
//      Log.d(TAG, "list: yBuffer1: "+yBuffer);

        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer(); // U
        //list: uBuffer: java.nio.DirectByteBuffer[pos=0 lim=153599 cap=153599]
//      Log.d(TAG, "list: uBuffer1: "+uBuffer);

        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer(); // V
        //list: vBuffer: java.nio.DirectByteBuffer[pos=0 lim=153599 cap=153599]
//      Log.d(TAG, "list: uBuffer1: "+vBuffer);

        int rowStride = image.getPlanes()[0].getRowStride();
//      Log.d(TAG, "list: rowStride[0]: "+rowStride);//640

        assert (image.getPlanes()[0].getPixelStride() == 1);

        int pos = 0;

        if (rowStride == width) { // likely
            yBuffer.get(nv21, 0, ySize);
            pos += ySize;
        } else {
            long yBufferPos = -rowStride; // not an actual position
            for (; pos < ySize; pos += width) {
                yBufferPos += rowStride;
                yBuffer.position((int) yBufferPos);
                yBuffer.get(nv21, pos, width);
            }
        }
        rowStride = image.getPlanes()[2].getRowStride();
        int pixelStride = image.getPlanes()[2].getPixelStride();
        assert (rowStride == image.getPlanes()[1].getRowStride());
        assert (pixelStride == image.getPlanes()[1].getPixelStride());

        if (pixelStride == 2 && rowStride == width && uBuffer.get(0) == vBuffer.get(1)) {
            // maybe V an U planes overlap as per NV21, which means vBuffer[1] is alias of uBuffer[0]
            byte savePixel = vBuffer.get(1);
            try {
                vBuffer.put(1, (byte) ~savePixel);
                if (uBuffer.get(0) == (byte) ~savePixel) {
                    vBuffer.put(1, savePixel);
                    vBuffer.position(0);
                    uBuffer.position(0);
                    vBuffer.get(nv21, ySize, 1);
                    uBuffer.get(nv21, ySize + 1, uBuffer.remaining());

                    return nv21; // shortcut
                }
            } catch (ReadOnlyBufferException ex) {
                // unfortunately, we cannot check if vBuffer and uBuffer overlap
            }

            // unfortunately, the check failed. We must save U and V pixel by pixel
            vBuffer.put(1, savePixel);
        }

        // other optimizations could check if (pixelStride == 1) or (pixelStride == 2),
        // but performance gain would be less significant

        for (int row = 0; row < height / 2; row++) {
            for (int col = 0; col < width / 2; col++) {
                int vuPos = col * pixelStride + row * rowStride;
                nv21[pos++] = vBuffer.get(vuPos);
                nv21[pos++] = uBuffer.get(vuPos);
            }
        }

        return nv21;
    }

    private Bitmap toBitmap(Image image) {
        System.out.println("list:2 toBitmap");

        byte[] nv21 = YUV_420_888toNV21(image);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();

        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }
//
//    /** Model loader */
//    @SuppressWarnings("deprecation")
//    private void loadModel() {
//        System.out.println("list:2 loadModel");
//        try {
//            //model name
//            String modelFile = "mobile_face_net.tflite";
//            tfLite = new Interpreter(loadModelFile(MainActivity.this, modelFile));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private MappedByteBuffer loadModelFile(Activity activity, String MODEL_FILE) throws IOException {
//        System.out.println("list:2 loadModelFile");
//        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_FILE);
//        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
//        FileChannel fileChannel = inputStream.getChannel();
//        long startOffset = fileDescriptor.getStartOffset();
//        long declaredLength = fileDescriptor.getDeclaredLength();
//        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
//    }
}