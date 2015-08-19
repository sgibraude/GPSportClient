package com.example.matant.gpsportclient.Controllers;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;

import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.matant.gpsportclient.AsyncResponse;
import com.example.matant.gpsportclient.Utilities.ErrorHandler;
import com.example.matant.gpsportclient.R;
import com.example.matant.gpsportclient.Utilities.MailSender;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class ForgotPassword extends Activity implements AsyncResponse {
    private Button forgotbtn;
    private EditText editxtemail;
    private Pattern regexPattern;
    private Matcher regMatcher;
    private ErrorHandler err;
    DBcontroller dbController;
    private static final String TAG_FLG = "flag";
    private static final String PASS_FLG= "password";
    Session session = null;
    private String mailAcountAuthenticationAddress="GPSport.braude@gmail.com",
            mailAcountAuthenticationPassword="123qweasdzxc123qwe";
    private ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forgot_password);
        editxtemail = (EditText)findViewById(R.id.editTextemail);
        forgotbtn = (Button)findViewById(R.id.buttonForgotP);
        err = new ErrorHandler();
        progress = new ProgressDialog(this);
        forgotbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!err.validateEmailAddress(editxtemail.getText().toString()))
                    editxtemail.setError("email is invalid");
                else
                    sendDataToDBController();
            }
        });
            }

        @Override
    public void handleResponse(String jsonStr) {

        Log.d("handleResponse", jsonStr);
        if (jsonStr != null) {
            try {
                JSONObject jsonObj = new JSONObject(jsonStr);
                String flg = jsonObj.getString(TAG_FLG);

                switch(flg)
                {
                    case "user":
                        editxtemail.setError("This user isn't exists");
                        break;
                    case "recovered":
                        MailSender mailSender = new MailSender();
                        mailSender.sendMailTo(editxtemail.getText().toString(),jsonObj.getString(PASS_FLG));
                        startActivity(new Intent(ForgotPassword.this, Login.class));
                        break;
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Log.d("ServiceHandler", "Couldn't get any data from the url");
        }
    }

            @Override
            public void sendDataToDBController() {

                String emailp = editxtemail.getText().toString();
                BasicNameValuePair tagreq = new BasicNameValuePair("tag", "forgotpassword");
                BasicNameValuePair emailparam = new BasicNameValuePair("email", emailp);
                List<NameValuePair> nameValuePairList = new ArrayList<NameValuePair>();
                nameValuePairList.add(tagreq);
                nameValuePairList.add(emailparam);
                dbController = new DBcontroller();
                dbController.delegate = this;
                dbController.execute(nameValuePairList);

            }

    @Override
    /**
     * presenting process dialog while sending request to server.
     */
    public void preProcess() {
        this.progress = ProgressDialog.show(this, "Forgot Password",
                    "Recovering your password...", true);

        }

}
