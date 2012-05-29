package org.openintents.wifiserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.UUID;

import org.openintents.wifiserver.preference.OIWiFiPreferencesActivity_;
import org.openintents.wifiserver.preference.OiWiFiPreferences_;
import org.openintents.wifiserver.webserver.ServerStatusListener;
import org.openintents.wifiserver.webserver.WebServer;
import org.openintents.wifiserver.webserver.WebServer.Status;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.util.Log;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Click;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.OptionsItem;
import com.googlecode.androidannotations.annotations.OptionsMenu;
import com.googlecode.androidannotations.annotations.ViewById;
import com.googlecode.androidannotations.annotations.sharedpreferences.Pref;

@EActivity(R.layout.main)
@OptionsMenu(R.menu.menu)
public class OIWiFiServerActivity extends Activity {

    private final static String  TAG                       = OIWiFiServerActivity.class.getSimpleName();
    
    @ViewById protected TextView     textWifiStatus;
    @ViewById protected TextView     textURL;
    @ViewById protected TextView     textPasswordShown;
    @ViewById protected TextSwitcher textSwitcherPassword;
    @ViewById protected ToggleButton toggleStartStopServer;
    
    @Pref protected OiWiFiPreferences_ prefs;

    private ConnectivityReceiver mConnectivityReceiver     = null;
    private WebServer mWebServer = null;

    
////////////////////////////////////////////////////////////////////////////////
//////////////////////////////   LIVECYCLE   ///////////////////////////////////  
////////////////////////////////////////////////////////////////////////////////
    
    @AfterViews
    protected void onCreate() {
        mConnectivityReceiver = new ConnectivityReceiver() {
            @Override
            public void onConnectionChanged(ConnectionType type) {
                if (type != ConnectionType.NET_WIFI)
                    wifiDisconnected();
                else
                    wifiConnected();
            }
        };
        this.registerReceiver(mConnectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopServer();
        if (mConnectivityReceiver != null)
            this.unregisterReceiver(mConnectivityReceiver);
    }
    
    private void wifiDisconnected() {
        textWifiStatus.setText(R.string.disconnected);
        if (toggleStartStopServer.isChecked())
            toggleStartStopServer.toggle();
        toggleStartStopServer.setEnabled(false);
        stopServer();
    }
    
    private void wifiConnected() {
        textWifiStatus.setText(R.string.connected);
        toggleStartStopServer.setEnabled(true);
    }
////////////////////////////////////////////////////////////////////////////////
/////////////////////////////   INTERACTION   //////////////////////////////////  
////////////////////////////////////////////////////////////////////////////////
    
    @Click
    protected void toggleStartStopServer() {
        if (toggleStartStopServer.isChecked())
            startServer();
        else
            stopServer();
    }
    
    @Click
    protected void textSwitcherPassword() {
        textSwitcherPassword.showNext();
    }
    
////////////////////////////////////////////////////////////////////////////////
////////////////////////////////   MENU   //////////////////////////////////////  
////////////////////////////////////////////////////////////////////////////////
    
    @OptionsItem
    protected void menuPreferences() {
        startActivity(new Intent(this, OIWiFiPreferencesActivity_.class));
    }

////////////////////////////////////////////////////////////////////////////////
///////////////////////////////   SERVER   /////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////
    
    private void startServer() {
        if (prefs.passwordEnable().get() && !prefs.customPasswordEnable().get()) {
            prefs.edit().randomPassword().put(UUID.randomUUID().toString().substring(0, 8)).apply();
        }
        
        if (mWebServer == null) {
            if (prefs.sslEnable().get())
                try {
                    mWebServer = new WebServer(prefs.sslPort().get(), true, getAssets().open("oi.bks"), "openintents".toCharArray());
                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                }
            else
                mWebServer = new WebServer(prefs.port().get());
            
            mWebServer.addListener(new ServerStatusListener() {
                
                @Override
                public void onStatusChanged(Status status, String msg) {
                    switch (status) {
                        case ERROR: Toast.makeText(OIWiFiServerActivity.this, "Server Error: "+msg, Toast.LENGTH_LONG).show(); break;
                        case STARTED: serverStarted(); break;
                        case STOPPED: serverStopped();    
                    }
                }
            });
        }
        
        mWebServer.start();
    }

    private void stopServer() {
        if (mWebServer != null)
            mWebServer.stop();
        mWebServer = null;
    }
    
    private void serverStopped() {
        toggleStartStopServer.setChecked(false);
        textSwitcherPassword.setEnabled(false);
        textURL.setText("");
    }
    
    private void serverStarted() {
        
        textURL.setText(buildURLString());

        if (prefs.passwordEnable().get()) {
            textSwitcherPassword.setEnabled(true);
            
            if (prefs.customPasswordEnable().get()) {
                textPasswordShown.setText(prefs.customPassword().get());
            } else {
                textPasswordShown.setText(prefs.randomPassword().get());
            }
        }
    }
    
    
////////////////////////////////////////////////////////////////////////////////
////////////////////////////////   UTIL   //////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////    
    
    private String buildURLString() {
        return (prefs.sslEnable().get() ? "https" : "http") + "://" + getDeviceIPAddress()+":"+mWebServer.getPort();
    }
    
    private String getDeviceIPAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(TAG, ex.toString());
        }

        return null;
    }
}