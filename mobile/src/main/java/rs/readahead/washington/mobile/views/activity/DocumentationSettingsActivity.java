package rs.readahead.washington.mobile.views.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.domain.entity.Server;
import rs.readahead.washington.mobile.domain.entity.TellaUploadServer;
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer;
import rs.readahead.washington.mobile.mvp.contract.ICollectBlankFormListRefreshPresenterContract;
import rs.readahead.washington.mobile.mvp.contract.ICollectServersPresenterContract;
import rs.readahead.washington.mobile.mvp.contract.IServersPresenterContract;
import rs.readahead.washington.mobile.mvp.contract.ITellaUploadServersPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.CollectBlankFormListRefreshPresenter;
import rs.readahead.washington.mobile.mvp.presenter.CollectServersPresenter;
import rs.readahead.washington.mobile.mvp.presenter.ServersPresenter;
import rs.readahead.washington.mobile.mvp.presenter.TellaUploadServersPresenter;
import rs.readahead.washington.mobile.domain.entity.ServerType;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.views.dialog.CollectServerDialogFragment;
import rs.readahead.washington.mobile.views.dialog.TellaUploadServerDialogFragment;
import timber.log.Timber;


public class DocumentationSettingsActivity extends CacheWordSubscriberBaseActivity implements
        IServersPresenterContract.IView,
        ICollectServersPresenterContract.IView,
        ITellaUploadServersPresenterContract.IView,
        ICollectBlankFormListRefreshPresenterContract.IView,
        CollectServerDialogFragment.CollectServerDialogHandler,
        TellaUploadServerDialogFragment.TellaUploadServerDialogHandler {
    @BindView(R.id.anonymous_switch)
    SwitchCompat anonymousSwitch;
    @BindView(R.id.collect_switch)
    SwitchCompat collectSwitch;
    @BindView(R.id.collect_servers_list)
    LinearLayout listView;
    @BindView(R.id.servers_layout)
    View serversLayout;
    @BindView(R.id.enable_collect_info)
    TextView collectSwitchInfo;
    @BindView(R.id.offline_mode)
    SwitchCompat offlineSwitch;
    @BindView(R.id.offline_switch_layout)
    View offlineSwitchLayout;

    private ServersPresenter serversPresenter;
    private CollectServersPresenter collectServersPresenter;
    private TellaUploadServersPresenter tellaUploadServersPresenter;
    private CollectBlankFormListRefreshPresenter refreshPresenter;
    private List<Server> servers;
    private AlertDialog dialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_documentation_settings);
        ButterKnife.bind(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.documentation);
        }

        setupAnonymousSwitch();
        setupCollectSwitch();
        setupOfflineSwitch();
        setupCollectSettingsView();

        servers = new ArrayList<>();

        serversPresenter = new ServersPresenter(this);

        collectServersPresenter = new CollectServersPresenter(this);
        collectServersPresenter.getCollectServers();

        tellaUploadServersPresenter = new TellaUploadServersPresenter(this);
        tellaUploadServersPresenter.getTUServers();

        createRefreshPresenter();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopPresenting();
        stopRefreshPresenter();

        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.add_server)
    public void manage(View view) {
        showCollectServerDialog(null);
        //showChooseServerDialog();
    }

    @Override
    public void showLoading() {
    }

    @Override
    public void hideLoading() {
    }

    @Override
    public void onTUServersLoaded(List<TellaUploadServer> tellaUploadServers) {
        listView.removeAllViews();
        this.servers.addAll(tellaUploadServers);
        createServerViews(servers);
    }

    @Override
    public void onLoadTUServersError(Throwable throwable) {

    }

    @Override
    public void onCreatedTUServer(TellaUploadServer server) {
        servers.add(server);
        listView.addView(getServerItem(server), servers.indexOf(server));
        showToast(R.string.ra_server_created);
    }

    @Override
    public void onCreateTUServerError(Throwable throwable) {
        showToast(R.string.ra_collect_server_remove_error);
    }

    @Override
    public void onRemovedTUServer(TellaUploadServer server) {
        servers.remove(server);
        listView.removeAllViews();
        createServerViews(servers);
        showToast(R.string.ra_server_removed);
    }

    @Override
    public void onRemoveTUServerError(Throwable throwable) {
        showToast(R.string.ra_collect_server_remove_error);
    }

    @Override
    public void onUpdatedTUServer(TellaUploadServer server) {
        int i = servers.indexOf(server);
        if (i != -1) {
            servers.set(i, server);
            listView.removeViewAt(i);
            listView.addView(getServerItem(server), i);
            showToast(R.string.ra_server_updated);
        }
    }

    @Override
    public void onUpdateTUServerError(Throwable throwable) {
        showToast(R.string.ra_collect_server_update_error);
    }

    @Override
    public void onServersLoaded(List<CollectServer> collectServers) {
        listView.removeAllViews();
        this.servers.addAll(collectServers);
        createServerViews(servers);
    }

    @Override
    public void onLoadServersError(Throwable throwable) {
        showToast(R.string.ra_collect_server_load_error);
    }

    @Override
    public void onCreatedServer(CollectServer server) {
        servers.add(server);
        listView.addView(getServerItem(server), servers.indexOf(server));
        showToast(R.string.ra_server_created);
        if (MyApplication.isConnectedToInternet(this)) {
            refreshPresenter.refreshBlankForms();
        }
    }

    @Override
    public void onCreateServerError(Throwable throwable) {
        showToast(R.string.ra_collect_server_create_error);
    }

    @Override
    public void onUpdatedServer(CollectServer server) {
        int i = servers.indexOf(server);

        if (i != -1) {
            servers.set(i, server);
            listView.removeViewAt(i);
            listView.addView(getServerItem(server), i);
            showToast(R.string.ra_server_updated);
        }
    }

    @Override
    public void onUpdateServerError(Throwable throwable) {
        showToast(R.string.ra_collect_server_update_error);
    }

    @Override
    public void onServersDeleted() {
        Preferences.setCollectServersLayout(false);
        servers.clear();
        listView.removeAllViews();
        setupCollectSettingsView();
        showToast(R.string.deleted_servers_and_forms);
    }

    @Override
    public void onServersDeletedError(Throwable throwable) {
    }

    @Override
    public void onRemovedServer(CollectServer server) {
        servers.remove(server);
        listView.removeAllViews();
        createServerViews(servers);
        showToast(R.string.ra_server_removed);
    }

    @Override
    public void onRemoveServerError(Throwable throwable) {
        showToast(R.string.ra_collect_server_remove_error);
    }

    @Override
    public void onRefreshBlankFormsError(Throwable error) {
        Timber.d(error, getClass().getName());
    }

    @Override
    public Context getContext() {
        return this;
    }

    /*private void showChooseServerDialog() {
        dialog = DialogsUtil.showServerChoosingDialog(this,
                serverType -> {
                    if (serverType == ServerType.ODK_COLLECT) {
                        showCollectServerDialog(null);
                    } else {
                        showTellaUploadServerDialog(null);
                    }
                    dialog.dismiss();
                });
    }*/

    private void editCollectServer(CollectServer server) {
        showCollectServerDialog(server);
    }

    private void editTUServer(TellaUploadServer server) {
        showTellaUploadServerDialog(server);
    }

    private void removeCollectServer(final CollectServer server) {
        dialog = DialogsUtil.showDialog(this,
                getString(R.string.ra_server_remove_confirmation_text),
                getString(R.string.ra_remove),
                getString(R.string.cancel),
                (dialog, which) -> {
                    collectServersPresenter.remove(server);
                    dialog.dismiss();
                }, null);
    }

    private void removeTUServer(final TellaUploadServer server) {
        dialog = DialogsUtil.showDialog(this,
                getString(R.string.delete_tus_server_info),
                getString(R.string.delete),
                getString(R.string.cancel),
                (dialog, which) -> {
                    tellaUploadServersPresenter.remove(server);
                    dialog.dismiss();
                }, null);
    }

    @Override
    public void onCollectServerDialogCreate(CollectServer server) {
        collectServersPresenter.create(server);
    }

    @Override
    public void onCollectServerDialogUpdate(CollectServer server) {
        collectServersPresenter.update(server);
    }

    private void showCollectServerDialog(@Nullable CollectServer server) {
        CollectServerDialogFragment.newInstance(server)
                .show(getSupportFragmentManager(), CollectServerDialogFragment.TAG);
    }

    private void showTellaUploadServerDialog(@Nullable TellaUploadServer server) {
        TellaUploadServerDialogFragment.newInstance(server)
                .show(getSupportFragmentManager(), TellaUploadServerDialogFragment.TAG);
    }

    private void stopPresenting() {
        if (collectServersPresenter != null) {
            collectServersPresenter.destroy();
            collectServersPresenter = null;
        }

        if (tellaUploadServersPresenter != null) {
            tellaUploadServersPresenter.destroy();
            tellaUploadServersPresenter = null;
        }

        if (serversPresenter != null) {
            serversPresenter.destroy();
            serversPresenter = null;
        }
    }

    private void setupAnonymousSwitch() {
        anonymousSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> Preferences.setAnonymousMode(!isChecked));
        anonymousSwitch.setChecked(!Preferences.isAnonymousMode());
    }

    private void setupCollectSettingsView() {
        if (Preferences.isCollectServersLayout()) {
            collectSwitch.setChecked(true);
            serversLayout.setVisibility(View.VISIBLE);
            offlineSwitchLayout.setVisibility(View.VISIBLE);
        } else {
            collectSwitch.setChecked(false);
            serversLayout.setVisibility(View.GONE);
            offlineSwitchLayout.setVisibility(View.GONE);
        }
    }

    private void setupCollectSwitch() {
        collectSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked && servers.size() > 0) {
                showCollectDisableDialog();
            } else {
                Preferences.setCollectServersLayout(isChecked);
                setupCollectSettingsView();
            }
        });
    }

    private void createServerViews(List<Server> servers) {
        for (Server server : servers) {
            View view = getServerItem(server);
            listView.addView(view, servers.indexOf(server));
        }
    }

    private View getServerItem(Server server) {
        LayoutInflater inflater = LayoutInflater.from(this);
        @SuppressLint("InflateParams")
        LinearLayout item = (LinearLayout) inflater.inflate(R.layout.collect_server_row_for_list, null);

        TextView name = item.findViewById(R.id.server_title);
        ImageView edit = item.findViewById(R.id.edit);
        ImageView remove = item.findViewById(R.id.delete);

        if (server != null) {
            name.setText(server.getName());

            name.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    null,
                    null,
                    getContext().getResources().getDrawable(
                            server.isChecked() ? R.drawable.ic_checked_green : R.drawable.watch_later_gray
                    ),
                    null);

            remove.setOnClickListener(v -> {
                if (server.getServerType() == ServerType.ODK_COLLECT) {
                    removeCollectServer((CollectServer) server);
                } else {
                    removeTUServer((TellaUploadServer) server);
                }
            });
            edit.setOnClickListener(v -> {
                if (server.getServerType() == ServerType.ODK_COLLECT) {
                    editCollectServer((CollectServer) server);
                } else {
                    editTUServer((TellaUploadServer) server);
                }
            });
        }
        item.setTag(servers.indexOf(server));
        return item;
    }

    private void showCollectDisableDialog() {
        String message = getString(R.string.disconnect_servers_info);

        dialog = DialogsUtil.showThreeOptionDialogWithTitle(this,
                message,
                getString(R.string.disconnect_servers),
                getString(R.string.hide),
                getString(R.string.cancel),
                getString(R.string.delete),
                (dialog, which) -> {  //hide
                    Preferences.setCollectServersLayout(false);
                    setupCollectSettingsView();
                    dialog.dismiss();
                },
                (dialog, which) -> {  //cancel
                    collectSwitch.setChecked(true);
                    dialog.dismiss();
                },
                (dialog, which) -> {   //delete
                    serversPresenter.deleteServers();
                    dialog.dismiss();
                });
    }

    private void setupOfflineSwitch() {
        offlineSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> Preferences.setOfflineMode(isChecked));
        offlineSwitch.setChecked(Preferences.isOfflineMode());
    }

    private void stopRefreshPresenter() {
        if (refreshPresenter != null) {
            refreshPresenter.destroy();
            refreshPresenter = null;
        }
    }

    private void createRefreshPresenter() {
        if (refreshPresenter == null) {
            refreshPresenter = new CollectBlankFormListRefreshPresenter(this);
        }
    }

    @Override
    public void onTellaUploadServerDialogCreate(TellaUploadServer server) {
        tellaUploadServersPresenter.create(server);
    }

    @Override
    public void onTellaUploadServerDialogUpdate(TellaUploadServer server) {
        tellaUploadServersPresenter.update(server);
    }
}
