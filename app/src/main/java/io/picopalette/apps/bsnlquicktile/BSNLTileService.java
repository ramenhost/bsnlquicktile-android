package io.picopalette.apps.bsnlquicktile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

/**
 * Created by ramkumar on 29/06/17.
 */

public class BSNLTileService extends TileService {

    String broadcastCommand = "am broadcast -a android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED";
    String[] decidingCommands = {"settings get global multi_sim_data_call"};
    String[] switchToBsnl = {"settings put global multi_sim_data_call 2", broadcastCommand, "svc data enable"};
    String[] switchToJio = {"settings put global multi_sim_data_call 1", broadcastCommand, "svc data enable", "svc data disable"};

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")){
                updateTile();
            }
        }
    };

    @Override
    public void onTileAdded() {
        updateTile();
    }

    @Override
    public void onCreate() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        registerReceiver(receiver, filter);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
    }

    private void updateTile() {
        try {
            Tile tile = getQsTile();
            String data_sim = runAsRoot(decidingCommands);
            Log.i("data_sim", data_sim);
            if(data_sim.matches("1")) {
                tile.setIcon(Icon.createWithResource(this, R.drawable.ic_bsnl_off_24dp));
            } else if(data_sim.matches("2")) {
                tile.setIcon(Icon.createWithResource(this, R.drawable.ic_bsnl_on_24dp));
            }
            tile.updateTile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick() {
        try {
            Tile tile = getQsTile();
            String data_sim = runAsRoot(decidingCommands);
            Log.i("data_sim", data_sim);
            if(data_sim.matches("1")) {
                tile.setIcon(Icon.createWithResource(this, R.drawable.ic_bsnl_on_24dp));
                runAsRoot(switchToBsnl);
            } else if(data_sim.matches("2")) {
                tile.setIcon(Icon.createWithResource(this, R.drawable.ic_bsnl_off_24dp));
                runAsRoot(switchToJio);
            }
            tile.updateTile();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String runAsRoot(String[] cmds) throws Exception{
        Process p = Runtime.getRuntime().exec("su");
        DataOutputStream os = new DataOutputStream(p.getOutputStream());
        for (String tmpCmd : cmds) {
            os.writeBytes(tmpCmd+"\n");
        }
        os.writeBytes("exit\n");
        os.flush();
        return getResultFromProcess(p);
    }

    private String getResultFromProcess(Process process) throws Exception{
        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
        StringBuilder result=new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            result.append(line);
        }
        return result.toString();
    }
}
