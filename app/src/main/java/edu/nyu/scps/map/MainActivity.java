package edu.nyu.scps.map;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class MainActivity extends ActionBarActivity {
    boolean heardFromServer = false;
    boolean pageFinished = false;
    WebView webView;
    String longString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GetTask getTask = new GetTask();
        getTask.execute();

        webView = (WebView)findViewById(R.id.webView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setVerticalScrollBarEnabled(false);

        webView.setWebViewClient(new WebViewClient() {
            //Called when the WebView has finished loading the page of HTML.
            @Override
            public void onPageFinished(WebView view, String url) {
                pageFinished = true;
                //Woolworth Building
                float latitude = 40.712222f;
                float longitude = -74.008056f;
                int zoom = 18;
                String javascript = String.format("javascript:mapFunction(%f, %f, %d)",
                        latitude, longitude, zoom);
                view.loadUrl(javascript);

                if (heardFromServer) {
                   pinIn();
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            //Called when the JavaScript alert function is called.
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                Toast toast = Toast.makeText(MainActivity.this, "onJsAlert " + url + " " + message,
                        Toast.LENGTH_LONG);
                toast.show();

                //Cause the JavaScript alert function to return.
                result.confirm();
                return true;
            }
        });

        webView.loadUrl("file:///android_asset/map.html");
    }

    private void pinIn() {

        String[] lines = longString.split("\n");

        for (String line : lines) {
            String[] field = line.split("\\|");
            String dba = field[0];
            String address = field[1] + " " + field[2];
            webView.loadUrl("javascript:markerFunction(\"" + address + "\")");
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class GetTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... t) {
            //Must be declared outside of try block,
            //so we can mention them in finally block.
            HttpURLConnection httpURLConnection = null;
            BufferedReader bufferedReader = null;

            try {
                URL url = new URL("http://i5.nyu.edu:3001/?zipcode=10279");
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.connect();

                InputStream inputStream = httpURLConnection.getInputStream();
                if (inputStream == null) {
                    return null;
                }

                StringBuffer buffer = new StringBuffer();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                bufferedReader = new BufferedReader(inputStreamReader);

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    return null;
                }
                return buffer.toString();
            } catch (IOException exception) {
                Log.e("myTag", "GetTask doInBackground", exception);
                return null;
            } finally {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (final IOException exception) {
                        Log.e("myTag", "GetTask doInBackground", exception);
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(String s) {
            longString = s;
            heardFromServer = true;
            if (pageFinished) {
                pinIn();
            }
        }
    }
}
