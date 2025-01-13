package com.example.shoppinglist;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class PartnerSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.partner);
    }

    public void onPartnerClick(View view) {
        // ניתן לזהות את השותף לפי ה-ID של הרשומה
        String selectedPartner = "";

        switch (view.getId()) {
            case R.id.imagePartner1:
            case R.id.namePartner1:
                selectedPartner = "Alice";
                break;

            case R.id.imagePartner2:
            case R.id.namePartner2:
                selectedPartner = "Bob";
                break;

            // הוסיפי טיפול בשותפים נוספים כאן
        }

        // הצגת הודעה עם הבחירה
        Toast.makeText(this, "בחרת את: " + selectedPartner, Toast.LENGTH_SHORT).show();
    }
}

