package com.pasadarider.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pasadarider.AuthenticationActivity;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Utils {
    public static void renderCartBadge(BottomNavigationView bottomNavigationView) {
        /*FirebaseAuth AUTH = FirebaseAuth.getInstance();
        FirebaseFirestore DB = FirebaseFirestore.getInstance();

        if (AUTH.getCurrentUser() == null) {
            return;
        }

        Query query =  DB.collection("carts").document(AUTH.getCurrentUser().getUid())
                .collection("items");

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                int count = 0;
                ArrayList<Integer> itemQuantities = new ArrayList<>();
                for (QueryDocumentSnapshot doc : value) {
                    if (doc.get("quantity") != null) {
                        count += Integer.parseInt(Objects.requireNonNull(doc.get("quantity")).toString());
                    }
                }

                if (count > 0) {
                    bottomNavigationView.getOrCreateBadge(R.id.miCart).setNumber(count);
                }
                else {
                    bottomNavigationView.removeBadge(R.id.miCart);
                }
            }
        });*/
    }

    public static double calculateFare(long distance, double minFare, double extraFarePerKm) {
        double fare = 0;
        if (distance <= 1000) {
            fare = minFare;
        }
        else {
            long extraKilometers = distance / 1000;
            fare = minFare + (extraKilometers * extraFarePerKm);
        }
        return fare;
    }

    public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = (DrawableCompat.wrap(drawable)).mutate();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public static String initialsFromName(String name) {
        StringBuilder initials = new StringBuilder();
        for (String s : name.split(" ")) {
            initials.append(s.charAt(0));
        }
        return initials.toString();
    }

    public static String capitalizeEachWord(String str) {
        StringBuilder builder = new StringBuilder();
        for (String s : str.split(" ")) {
            String cap = s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
            builder.append(cap + " ");
        }
        return builder.toString();
    }

    public static String addressSeparator(String address) {
        StringBuilder addressStringBuilder = new StringBuilder();
        String[] addressArray = address.split("\\s*,\\s*");
        for (String addr : addressArray) {
            addressStringBuilder.append(addr).append(",").append("\n");
        }
        addressStringBuilder.setLength(addressStringBuilder.length() - 2);
        return addressStringBuilder.toString();
    }

    public static void hideKeyboard(Activity activity) {
        View view1 = activity.getCurrentFocus();
        if (view1 != null) {
            InputMethodManager imm = (InputMethodManager)activity.getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view1.getWindowToken(), 0);
        }
    }

    public static int randomNumberBetween(int min, int max) {
        return new Random().nextInt((max - min) + 1) + min;
    }

    public static float dpToPx(Context context, float valueInDp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics);
    }
    public static void basicDialog(Context context, String title, String button){
        MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(context);
        materialAlertDialogBuilder.setTitle(title);
        materialAlertDialogBuilder.setPositiveButton(button, (dialogInterface, i) -> { });
        materialAlertDialogBuilder.show();
    }
    public static void simpleDialog(Context context, String title, String message, String button){
        MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(context);
        materialAlertDialogBuilder.setTitle(title);
        materialAlertDialogBuilder.setMessage(message);
        materialAlertDialogBuilder.setPositiveButton(button, (dialogInterface, i) -> { });
        materialAlertDialogBuilder.show();
    }
    public static void loginRequiredDialog(Context context, BottomNavigationView bottom_navbar, String message){
        MaterialAlertDialogBuilder dialogLoginRequired = new MaterialAlertDialogBuilder(context);
        dialogLoginRequired.setTitle("Sign in required");
        dialogLoginRequired.setMessage(message);
        dialogLoginRequired.setPositiveButton("Log in", (dialogInterface, i) -> {
            context.startActivity(new Intent(context, AuthenticationActivity.class));
            ((Activity)context).finish();
        });
        dialogLoginRequired.setNeutralButton("Back", (dialogInterface, i) -> { });
        dialogLoginRequired.setOnDismissListener(dialogInterface -> bottom_navbar.getMenu().getItem(0).setChecked(true));
        dialogLoginRequired.show();
    }

    public static class Cache {
        public static void removeKey(Context context, String key){
            SharedPreferences sharedPreferences = context.getSharedPreferences("fixcare_cache", Context.MODE_PRIVATE);
            sharedPreferences.edit().remove(key).apply();
        }

        public static String getString(Context context, String key){
            SharedPreferences sharedPreferences = context.getSharedPreferences("fixcare_cache", Context.MODE_PRIVATE);
            return sharedPreferences.getString(key, "");
        }

        public static void setString(Context context, String key, String value){
            SharedPreferences sharedPreferences = context.getSharedPreferences("fixcare_cache", Context.MODE_PRIVATE);
            sharedPreferences.edit().putString(key, value).apply();
        }

        public static int getInt(Context context, String key){
            SharedPreferences sharedPreferences = context.getSharedPreferences("fixcare_cache", Context.MODE_PRIVATE);
            return sharedPreferences.getInt(key, 0);
        }

        public static void setInt(Context context, String key, int value){
            SharedPreferences sharedPreferences = context.getSharedPreferences("fixcare_cache", Context.MODE_PRIVATE);
            sharedPreferences.edit().putInt(key, value).apply();
        }

        public static double getDouble(Context context, String key){
            SharedPreferences sharedPreferences = context.getSharedPreferences("fixcare_cache", Context.MODE_PRIVATE);
            return Double.longBitsToDouble(sharedPreferences.getLong(key, 0));
        }

        public static void setDouble(Context context, String key, double value){
            SharedPreferences sharedPreferences = context.getSharedPreferences("fixcare_cache", Context.MODE_PRIVATE);
            sharedPreferences.edit().putLong(key, Double.doubleToRawLongBits(value)).apply();
        }

        public static long getLong(Context context, String key){
            SharedPreferences sharedPreferences = context.getSharedPreferences("fixcare_cache", Context.MODE_PRIVATE);
            return sharedPreferences.getLong(key, 0);
        }

        public static void setLong(Context context, String key, long value){
            SharedPreferences sharedPreferences = context.getSharedPreferences("fixcare_cache", Context.MODE_PRIVATE);
            sharedPreferences.edit().putLong(key, value).apply();
        }

        public static boolean getBoolean(Context context, String key){
            SharedPreferences sharedPreferences = context.getSharedPreferences("fixcare_cache", Context.MODE_PRIVATE);
            return sharedPreferences.getBoolean(key, false);
        }

        public static void setBoolean(Context context, String key, boolean value){
            SharedPreferences sharedPreferences = context.getSharedPreferences("fixcare_cache", Context.MODE_PRIVATE);
            sharedPreferences.edit().putBoolean(key, value).apply();
        }

        public static Set<String> getStringSet(Context context, String key){
            SharedPreferences sharedPreferences = context.getSharedPreferences("fixcare_cache", Context.MODE_PRIVATE);
            return sharedPreferences.getStringSet(key, new HashSet<>());
        }

        public static void setStringSet(Context context, String key, Set<String> set){
            SharedPreferences sharedPreferences = context.getSharedPreferences("fixcare_cache", Context.MODE_PRIVATE);
            sharedPreferences.edit().putStringSet(key, set).apply();
        }
    }

    public static class DoubleFormatter {

        public static String currencyFormat(double dbl){
            if (dbl == 0) {
                return "0.00";
            }
            else {
                DecimalFormat formatter = new DecimalFormat("#,###.00");
                return formatter.format(dbl);
            }
        }

    }
}
