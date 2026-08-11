package android.print;

import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;

import java.io.File;

/**
 * Drives the platform {@link PrintDocumentAdapter} to write a PDF to a file.
 *
 * Lives in the {@code android.print} package on purpose: the constructors of
 * {@code LayoutResultCallback} and {@code WriteResultCallback} are package-private,
 * so they can only be subclassed from within this package.
 */
public final class PdfWriter {

    public interface Callback {
        void onDone();
        void onError(Throwable t);
    }

    public static void write(final PrintDocumentAdapter adapter, final File outFile, final Callback cb) {
        PrintAttributes attributes = new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(new PrintAttributes.Resolution("pdf", "pdf", 600, 600))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build();

        adapter.onLayout(null, attributes, null, new PrintDocumentAdapter.LayoutResultCallback() {
            @Override
            public void onLayoutFinished(PrintDocumentInfo info, boolean changed) {
                final ParcelFileDescriptor fd;
                try {
                    fd = ParcelFileDescriptor.open(
                            outFile,
                            ParcelFileDescriptor.MODE_READ_WRITE
                                    | ParcelFileDescriptor.MODE_CREATE
                                    | ParcelFileDescriptor.MODE_TRUNCATE);
                } catch (Throwable t) {
                    cb.onError(t);
                    return;
                }
                adapter.onWrite(new PageRange[]{PageRange.ALL_PAGES}, fd, new CancellationSignal(),
                        new PrintDocumentAdapter.WriteResultCallback() {
                            @Override
                            public void onWriteFinished(PageRange[] pages) {
                                closeQuietly(fd);
                                cb.onDone();
                            }

                            @Override
                            public void onWriteFailed(CharSequence error) {
                                closeQuietly(fd);
                                cb.onError(new IllegalStateException("PDF write failed: " + error));
                            }
                        });
            }

            @Override
            public void onLayoutFailed(CharSequence error) {
                cb.onError(new IllegalStateException("PDF layout failed: " + error));
            }
        }, null);
    }

    private static void closeQuietly(ParcelFileDescriptor fd) {
        try { fd.close(); } catch (Throwable ignored) { }
    }

    private PdfWriter() { }
}
