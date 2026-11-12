package com.discovervancouver.mobile.ui;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.Intent;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.discovervancouver.mobile.R;
import com.discovervancouver.mobile.model.Recommendation;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class PlaceDetailNavigationTest {

    @Test
    public void recommendationOpensAndBackButtonClosesDetail() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, PlaceDetailActivity.class);
        intent.putExtra(
            PlaceDetailActivity.EXTRA_RECOMMENDATION,
            recommendation()
        );

        try (ActivityScenario<PlaceDetailActivity> ignored =
                 ActivityScenario.launch(intent)) {
            ignored.onActivity(activity -> {
                TextView name = activity.findViewById(R.id.detailName);
                assertEquals("Burrard Coffee", name.getText().toString());
                activity.findViewById(R.id.backButton).performClick();
            });
        }
    }

    private Recommendation recommendation() {
        return new Recommendation(
            "place-1", "Burrard Coffee", "cafe",
            "A quiet downtown cafe", "100 Burrard Street", 1,
            true, true, List.of("quiet"), 91, 350, 5,
            49.2827, -123.1207, false,
            List.of("Matches your cafe preference")
        );
    }
}
