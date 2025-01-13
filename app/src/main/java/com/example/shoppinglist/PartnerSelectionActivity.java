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
        // ניתן לזהות את השותף לפי ה-ID של הרששומה
        String selectedPartner = "";

        if (view.getId() == R.id.imagePartner1 || view.getId() == R.id.namePartner1) {
            selectedPartner = "Alice";
        } else if (view.getId() == R.id.imagePartner2 || view.getId() == R.id.namePartner2) {
            selectedPartner = "Bob";
        }
        // הוסיפי טיפול בשותפים נוספים כאן

        // הצגת הודעה עם הבחירה
        Toast.makeText(this, "בחרת את: " + selectedPartner, Toast.LENGTH_SHORT).show();
    }

}

