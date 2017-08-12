package org.secuso.privacyfriendlytraining.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.secuso.privacyfriendlytraining.tutorial.TutorialActivity;

/**
 * Created by yonjuni on 22.10.16.
 * @license GNU/GPLv3 http://www.gnu.org/licenses/gpl-3.0.html
 */

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent mainIntent = new Intent(SplashActivity.this, TutorialActivity.class);
        SplashActivity.this.startActivity(mainIntent);
        SplashActivity.this.finish();

    }

}
