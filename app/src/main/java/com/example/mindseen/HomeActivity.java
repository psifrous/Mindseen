package com.example.mindseen;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.microsoft.projectoxford.emotion.EmotionServiceClient;
import com.microsoft.projectoxford.emotion.EmotionServiceRestClient;
import com.microsoft.projectoxford.emotion.contract.RecognizeResult;
import com.microsoft.projectoxford.emotion.contract.Scores;
import com.microsoft.projectoxford.emotion.rest.EmotionServiceException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
     Button btnLogout;
     FirebaseAuth mFirebaseAuth;
     private FirebaseAuth.AuthStateListener mAuthStateListener;
     ImageView imageView;
     Button btnTakePicture,btnProcess;

     EmotionServiceClient restClient = new EmotionServiceRestClient("a9c0f8d7385c4db0af1bb2ecf12ebd2c");
    int TAKE_PICTURE_CODE = 100;
    int REQUEST_PERMISSION_CODE = 101;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if(requestCode == REQUEST_PERMISSION_CODE){
            if(grantResults[0]== PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();

        }
    }

    Bitmap mBitmap;
    TextView TxtOtp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initViews();

        if((checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED ) && (checkSelfPermission(Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED))
        {
            requestPermissions(new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET
            },REQUEST_PERMISSION_CODE);
            }
    }

    private void initViews() {
        btnProcess = (Button)findViewById(R.id.btnProcess);
        btnTakePicture = (Button)findViewById(R.id.btnTakePicture);
        imageView = (ImageView)findViewById(R.id.imageView);

        btnTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePicFromGallery();
            }
        });
        btnProcess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                processImage();
            }
        });

    }

    private void processImage() {
        btnLogout = findViewById(R.id.logout);
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Intent intToMain = new Intent(HomeActivity.this, MainActivity.class);
                startActivity(intToMain);
            }
        });

        TxtOtp = (TextView)findViewById(R.id.TxtOtp);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG,30,outputStream);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());


        @SuppressLint("StaticFieldLeak") AsyncTask<InputStream,String, List<RecognizeResult>>  processAsync = new AsyncTask<InputStream, String, List<RecognizeResult>>() {

            @Override
            protected void onPostExecute(List<RecognizeResult> recognizeResults) {
            for(RecognizeResult res : recognizeResults)
            {
                String status = getEmotion(res);
                TxtOtp.setText(status);
            }
            }

            @Override
            protected List<RecognizeResult> doInBackground(InputStream... inputStreams) {
                List<RecognizeResult> result = null;
                try{
                    result = restClient.recognizeImage(inputStreams[0]);
                }
                catch (EmotionServiceException | IOException e)
                {
                    e.printStackTrace();;
                }
                return result;
            }
        };
        processAsync.execute(inputStream);
    }

    private String getEmotion(RecognizeResult res) {
        List<Double> list = new ArrayList<>();
        Scores scores = res.scores;

        list.add(scores.anger);
        list.add(scores.happiness);
        list.add(scores.contempt);
        list.add(scores.fear);
        list.add(scores.neutral);
        list.add(scores.sadness);

        Collections.sort(list);
        double maxNum = list.get(list.size() - 1);
        if(maxNum == scores.anger)
            return "Anger - Anger and intolerance are the enemies of correct understanding. Mahatma Gandhi.";
        else if (maxNum == scores.happiness)
            return "Happiness - life’s better when we’re happy, healthy, and successful.";
        else if (maxNum == scores.contempt)
            return "Contempt - Many can bear adversity, but few contempt";
        else if (maxNum == scores.fear)
            return "Fear - Fear defeats more people than any other one thing in the world.";
        else if (maxNum == scores.neutral)
            return "Neutral - Sometimes all we want is to be out of emotions";
        else if (maxNum == scores.sadness)
            return "Sadness - Sadness flies away on the wings of time.";
        else
            return "Can't detect emotion";
    }

  @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TAKE_PICTURE_CODE) {
            Uri selectedImageUri = data.getData();
            InputStream in = null;
            try {
                in = getContentResolver().openInputStream(selectedImageUri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            mBitmap = BitmapFactory.decodeStream(in);
            imageView.setImageBitmap(mBitmap);
        }


    }

    private void takePicFromGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(intent.ACTION_GET_CONTENT);
        startActivityForResult(intent,TAKE_PICTURE_CODE);

    }
}
