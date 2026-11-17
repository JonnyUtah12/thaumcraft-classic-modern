package com.jonnyutah12.thaumcraftclassic.research.book;

/**
 * The seam that displays the book (TC-M3-03-s): whatever is installed here is
 * what "opening the Thaumonomicon" means on the current side.
 *
 * <p>The presentation state machine in {@link ThaumonomiconBook} is
 * server-safe, but the thing that actually shows a book is client-only. Rather
 * than let the shared code reach for a client class (a dedicated-server
 * classloading hazard: {@code runGameTestServer} and {@code runServer} must
 * never load {@code Screen}), the shared code calls
 * {@link #openBook(ThaumonomiconBook)} and the client installs the real
 * implementation at startup.</p>
 *
 * <p>This also gives the game test a meaningful subject: a probe installs a
 * recording host, drives the real open path, and asserts on the
 * {@link BookFrame} the host was handed — which is exactly what the screen
 * would draw, without a client in the loop.</p>
 */
public interface ThaumonomiconHost {

    /** Display the book at the state the controller is in. */
    void open(ThaumonomiconBook book);

    /** The installed host, or null when nothing on this side can display the book. */
    static ThaumonomiconHost installed() {
        return Holder.HOST;
    }

    /** Install the display implementation (the client does this at startup). */
    static void install(final ThaumonomiconHost host) {
        Holder.HOST = host;
    }

    /** Remove the installed host (client shutdown, and game-test cleanup). */
    static void uninstall() {
        Holder.HOST = null;
    }

    /**
     * Open the book through the installed host.
     *
     * @return false when nothing can display it — on a dedicated server, or
     *     before the client has installed its host. The caller must not treat
     *     that as an error: the state machine stays usable either way.
     */
    static boolean openBook(final ThaumonomiconBook book) {
        final ThaumonomiconHost host = Holder.HOST;
        if (host == null) {
            return false;
        }
        host.open(book);
        return true;
    }

    /** Volatile holder so a client thread sees the install without extra locking. */
    final class Holder {
        private static volatile ThaumonomiconHost HOST;

        private Holder() {
        }
    }
}
