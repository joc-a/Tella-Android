package rs.readahead.washington.mobile.mvp.presenter;

import com.crashlytics.android.Crashlytics;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.mvp.contract.ITellaFileUploadPresenterContract;


public class TellaFileUploadPresenter implements ITellaFileUploadPresenterContract.IPresenter {
    private ITellaFileUploadPresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();
    private CacheWordDataSource cacheWordDataSource;


    public TellaFileUploadPresenter(ITellaFileUploadPresenterContract.IView view) {
        this.view = view;
        this.cacheWordDataSource = new CacheWordDataSource(view.getContext());
    }

    @Override
    public void destroy() {
        disposables.dispose();
        cacheWordDataSource.dispose();
        view = null;
    }

    @Override
    public void scheduleUploadMediaFiles(final List<MediaFile> mediaFiles) {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapCompletable(dataSource -> dataSource.scheduleUploadMediaFiles(mediaFiles))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> view.onMediaFilesUploadScheduled(), throwable -> {
                    Crashlytics.logException(throwable);
                    view.onMediaFilesUploadScheduleError(throwable);
                })
        );
    }
}