package com.translateje.app;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.content.*;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import org.json.*;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {

    LinearLayout root, content;
    TextToSpeech tts;
    ArrayList<Phrase> all = new ArrayList<>();
    Set<String> fav = new HashSet<>();

    String[] cats = {
        "🍜 Food",
        "🚕 Transport",
        "🛍 Shopping",
        "🏨 Hotel",
        "🗺 Directions",
        "🛬 Airport",
        "🚆 Train",
        "🚨 Emergency",
        "💬 Basic"
    };

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );

        tts = new TextToSpeech(this, this);

        fav = new HashSet<>(
                getSharedPreferences("translateje", 0)
                        .getStringSet("fav", new HashSet<>())
        );

        load();
        home();
    }

    void load() {
        try {
            BufferedReader r = new BufferedReader(
                    new InputStreamReader(
                            getAssets().open("phrases.json"),
                            "UTF-8"
                    )
            );

            StringBuilder s = new StringBuilder();
            String l;

            while ((l = r.readLine()) != null) {
                s.append(l);
            }

            JSONArray a = new JSONArray(s.toString());

            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);

                all.add(new Phrase(
                        o.getString("id"),
                        o.getString("category"),
                        o.getString("malay"),
                        o.getString("thai"),
                        o.getString("romanization")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    TextView tv(String s, int size) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(size);
        v.setTextColor(Color.rgb(25, 25, 25));
        v.setPadding(20, 18, 20, 18);
        return v;
    }

    Button btn(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        return b;
    }

    void base(String title) {

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        LinearLayout bar = new LinearLayout(this);
        bar.setPadding(12, 12, 12, 4);

        TextView back = tv("‹", 34);

        back.setOnClickListener(v -> home());

        bar.addView(
                back,
                new LinearLayout.LayoutParams(55, 65)
        );

        TextView t = tv(title, 24);
        t.setTypeface(null, 1);

        bar.addView(
                t,
                new LinearLayout.LayoutParams(0, 65, 1)
        );

        root.addView(bar);

        ScrollView sc = new ScrollView(this);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(18, 5, 18, 30);

        sc.addView(content);

        root.addView(
                sc,
                new LinearLayout.LayoutParams(-1, 0, 1)
        );

        setContentView(root);
    }

    void home() {

        base("TranslateJe");

        content.addView(
                tv("Tap. Speak. Travel.", 15)
        );

        TextView h = tv("What do you need?", 27);
        h.setTypeface(null, 1);
        content.addView(h);

        String[] quick = {
                "How much?",
                "Not spicy",
                "Take me here",
                "Thank you",
                "I don't understand"
        };

        for (String q : quick) {

            Button b = btn("⚡ " + q);
            content.addView(b);

            b.setOnClickListener(v -> {

                for (Phrase p : all) {

                    if (
                            (q.equals("How much?")
                                    && p.malay.equals("Berapa harga?"))

                            || (q.equals("Not spicy")
                                    && p.malay.equals("Tak nak pedas."))

                            || (q.equals("Take me here")
                                    && p.malay.startsWith("Tolong hantar"))

                            || (q.equals("Thank you")
                                    && p.malay.equals("Terima kasih."))

                            || (q.equals("I don't understand")
                                    && p.malay.equals("Saya tak faham."))
                    ) {
                        phrase(p);
                        break;
                    }
                }
            });
        }

        content.addView(tv("Categories", 22));

        for (String c : cats) {

            Button b = btn(c);
            content.addView(b);

            b.setOnClickListener(
                    v -> category(c.substring(2))
            );
        }

        Button em = btn("🚨 EMERGENCY");
        content.addView(em);

        em.setOnClickListener(v -> emergency());

        Button favb = btn("⭐ My Trip (Saved)");
        content.addView(favb);

        favb.setOnClickListener(v -> favorites());

        Button search = btn("🔎 Search phrases");
        content.addView(search);

        search.setOnClickListener(v -> search());
    }

    void category(String c) {

        base(c);

        for (Phrase p : all) {

            if (p.cat.equals(c)) {

                Button b = btn(p.malay);
                content.addView(b);

                b.setOnClickListener(v -> phrase(p));
            }
        }
    }

    void phrase(Phrase p) {

        base("Phrase");

        TextView m = tv(p.malay, 28);
        m.setTypeface(null, 1);
        content.addView(m);

        content.addView(tv("Thai", 13));

        TextView th = tv(p.thai, 32);
        th.setGravity(Gravity.CENTER);
        content.addView(th);

        content.addView(
                tv("Pronunciation\n" + p.rom, 18)
        );

        Button play = btn("🔊 PLAY PHRASE");
        content.addView(play);

        play.setOnClickListener(
                v -> speak(p.thai)
        );

        Button show = btn("📱 SHOW TO LOCAL");
        content.addView(show);

        show.setOnClickListener(
                v -> showLocal(p)
        );

        Button save = btn(
                fav.contains(p.id)
                        ? "★ SAVED"
                        : "☆ SAVE TO MY TRIP"
        );

        content.addView(save);

        save.setOnClickListener(v -> {

            if (fav.contains(p.id)) {
                fav.remove(p.id);
            } else {
                fav.add(p.id);
            }

            savePrefs();

            save.setText(
                    fav.contains(p.id)
                            ? "★ SAVED"
                            : "☆ SAVE TO MY TRIP"
            );
        });
    }

    void showLocal(Phrase p) {

        base("Show to Local");

        Space sp = new Space(this);

        content.addView(
                sp,
                new LinearLayout.LayoutParams(1, 0, 1)
        );

        TextView th = tv(p.thai, 38);
        th.setGravity(Gravity.CENTER);
        th.setTypeface(null, 1);

        content.addView(th);

        Button b = btn("🔊 PLAY");
        content.addView(b);

        b.setOnClickListener(
                v -> speak(p.thai)
        );

        Space sp2 = new Space(this);

        content.addView(
                sp2,
                new LinearLayout.LayoutParams(1, 0, 1)
        );
    }

    void favorites() {

        base("My Trip");

        for (Phrase p : all) {

            if (fav.contains(p.id)) {

                Button b = btn(p.malay);
                content.addView(b);

                b.setOnClickListener(
                        v -> phrase(p)
                );
            }
        }

        if (fav.isEmpty()) {

            content.addView(
                    tv(
                            "No saved phrases yet. Tap ☆ SAVE on any phrase.",
                            18
                    )
            );
        }
    }

    void search() {

        base("Search");

        EditText e = new EditText(this);
        e.setHint("Type Malay phrase...");
        content.addView(e);

        Button b = btn("Search");
        content.addView(b);

        LinearLayout results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);

        content.addView(results);

        b.setOnClickListener(v -> {

            results.removeAllViews();

            String q = e.getText()
                    .toString()
                    .toLowerCase();

            for (Phrase p : all) {

                if (
                        p.malay.toLowerCase().contains(q)
                                || p.thai.contains(q)
                ) {

                    Button x = btn(p.malay);
                    results.addView(x);

                    x.setOnClickListener(
                            z -> phrase(p)
                    );
                }
            }
        });
    }

    void emergency() {

        base("🚨 Emergency");

        String[][] x = {
                {"POLICE", "191"},
                {"AMBULANCE / MEDICAL", "1669"},
                {"TOURIST POLICE", "1155"}
        };

        for (String[] a : x) {

            TextView n = tv(
                    a[0] + "\n" + a[1],
                    25
            );

            n.setGravity(Gravity.CENTER);
            content.addView(n);

            Button c = btn("CALL " + a[1]);
            content.addView(c);

            c.setOnClickListener(v -> {

                Intent i = new Intent(Intent.ACTION_DIAL);

                i.setData(
                        android.net.Uri.parse(
                                "tel:" + a[1]
                        )
                );

                startActivity(i);
            });
        }

        content.addView(
                tv("Emergency phrases", 20)
        );

        for (Phrase p : all) {

            if (p.cat.equals("Emergency")) {

                Button q = btn(p.malay);
                content.addView(q);

                q.setOnClickListener(
                        v -> phrase(p)
                );
            }
        }
    }

    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {

            tts.setLanguage(
                    new Locale("th", "TH")
            );
        }
    }

    void speak(String s) {

        if (tts != null) {

            tts.speak(
                    s,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "translateje"
            );
        }
    }

    void savePrefs() {

        getSharedPreferences(
                "translateje",
                0
        )
                .edit()
                .putStringSet("fav", fav)
                .apply();
    }

    static class Phrase {

        String id;
        String cat;
        String malay;
        String thai;
        String rom;

        Phrase(
                String a,
                String b,
                String c,
                String d,
                String e
        ) {
            id = a;
            cat = b;
            malay = c;
            thai = d;
            rom = e;
        }
    }
              }
