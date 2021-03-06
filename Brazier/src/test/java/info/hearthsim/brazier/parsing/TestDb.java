package info.hearthsim.brazier.parsing;

import info.hearthsim.brazier.db.HearthStoneDb;
import org.jtrim.utils.ExceptionHelper;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public final class TestDb {
    private static final AtomicReference<Throwable> LOAD_FAILURE = new AtomicReference<>(null);
    private static final AtomicReference<HearthStoneDb> DB_REF = new AtomicReference<>(null);

    public static HearthStoneDb getTestDbUnsafe() throws IOException, ObjectParsingException {
        Throwable failure = LOAD_FAILURE.get();
        if (failure != null) {
            throw ExceptionHelper.throwChecked(failure, ObjectParsingException.class);
        }

        HearthStoneDb result = DB_REF.get();
        if (result == null) {
            try {
                result = HearthStoneDb.readDefault();
            } catch (Throwable ex) {
                LOAD_FAILURE.set(ex);
                throw ex;
            }
            if (!DB_REF.compareAndSet(null, result)) {
                result = DB_REF.get();
            }
        }
        return result;
    }

    public static HearthStoneDb getTestDb() {
        try {
            return getTestDbUnsafe();
        } catch (IOException | ObjectParsingException ex) {
            IllegalStateException toThrow = new IllegalStateException("TestDb is not available.", ex);
            toThrow.setStackTrace(new StackTraceElement[0]);
            throw toThrow;
        }
    }

    private TestDb() {
        throw new AssertionError();
    }
}
