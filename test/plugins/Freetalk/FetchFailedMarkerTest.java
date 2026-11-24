/* This code is part of Freenet. It is distributed under the GNU General
 * Public License, version 2 (or at your option any later version). See
 * http://www.gnu.org/ for further details of the GPL. */
package plugins.Freetalk;

import java.lang.reflect.Field;
import java.util.Date;

import com.db4o.ObjectSet;
import com.db4o.ext.StoredClass;
import com.db4o.query.Query;

import plugins.Freetalk.FetchFailedMarker.Reason;
import plugins.Freetalk.util.CurrentTimeUTC;

/**
 * Tests whether class FetchFailedMarker survives being stored in the database, especially its {@link Reason}: The reason was once stored as an enum,
 * which does not work on Java 16 and later if the JVM is not launched with "--add-opens java.base/java.lang=ALL-UNNAMED".
 * Thus this test should be run without that parameter.
 */
public final class FetchFailedMarkerTest extends DatabaseBasedTest {

	private FetchFailedMarker store(final Reason reason) {
		final Date now = CurrentTimeUTC.get();
		final FetchFailedMarker marker = new FetchFailedMarker(reason, now, new Date(now.getTime() + 60 * 60 * 1000));
		marker.initializeTransient(mFreetalk);
		storeAndCommit(marker);
		return marker;
	}

	private void storeAndCommit(final FetchFailedMarker marker) {
		synchronized(Persistent.transactionLock(db)) {
			try {
				marker.storeWithoutCommit();
				Persistent.checkedCommit(db, this);
			} catch(RuntimeException e) {
				Persistent.checkedRollbackAndThrow(db, this, e);
			}
		}
	}

	/** Closes and re-opens the database to ensure that objects are really loaded from disk instead of being taken from the db4o object cache. */
	private void reopenDatabase() {
		while(!db.close());
		mFreetalk = new Freetalk(getDatabaseFilename());
		db = mFreetalk.getDatabase();
	}

	private ObjectSet<FetchFailedMarker> getMarkers() {
		final Query q = db.query();
		q.constrain(FetchFailedMarker.class);
		return new Persistent.InitializingObjectSet<FetchFailedMarker>(mFreetalk, q);
	}

	private FetchFailedMarker getSingleMarker() {
		final ObjectSet<FetchFailedMarker> markers = getMarkers();
		assertEquals(1, markers.size());
		return markers.next();
	}

	private static Field getReasonCodeField() throws Exception {
		final Field field = FetchFailedMarker.class.getDeclaredField("mReasonCode");
		field.setAccessible(true);
		return field;
	}

	public void testConstructorAndSetReason() {
		try {
			new FetchFailedMarker(null, new Date(), new Date());
			fail("The constructor should not accept null as reason");
		} catch(NullPointerException e) { }

		final FetchFailedMarker marker = store(Reason.DataNotFound);

		try {
			marker.setReason(null);
			fail("setReason() should not accept null");
		} catch(NullPointerException e) { }

		assertEquals(Reason.DataNotFound, marker.getReason());
	}

	public void testReasonIsPersistent() throws Exception {
		for(final Reason reason : Reason.values()) {
			store(reason);
			reopenDatabase();

			final FetchFailedMarker marker = getSingleMarker();
			assertEquals(reason, marker.getReason()); // Must activate on its own
			marker.databaseIntegrityTest();

			synchronized(Persistent.transactionLock(db)) {
				marker.deleteWithoutCommit();
				Persistent.checkedCommit(db, this);
			}
			assertEquals(0, getMarkers().size());
		}
	}

	public void testSetReasonIsPersistent() throws Exception {
		store(Reason.Unknown);

		for(final Reason reason : Reason.values()) {
			reopenDatabase();
			FetchFailedMarker marker = getSingleMarker();
			marker.setReason(reason);
			storeAndCommit(marker);

			reopenDatabase();
			marker = getSingleMarker();
			assertEquals(reason, marker.getReason());
			marker.databaseIntegrityTest();
		}
	}

	/** As opposed to the other tests this will also notice that enums are being stored if the Java version or JVM parameters do allow it. */
	public void testNoEnumIsStored() {
		store(Reason.ParsingFailed);

		for(final StoredClass storedClass : db.storedClasses()) {
			assertFalse(Enum.class.getName().equals(storedClass.getName()));
			assertFalse(Reason.class.getName().equals(storedClass.getName()));
		}
	}

	/** If this fails you've changed what existing databases contain, so {@link Freetalk#DATABASE_FORMAT_VERSION} has to be incremented. */
	public void testReasonCodesAreStable() throws Exception {
		final Field code = getReasonCodeField();
		final Date date = new Date();

		assertEquals(3, Reason.values().length);
		assertEquals("Unknown", code.get(new FetchFailedMarker(Reason.Unknown, date, date)));
		assertEquals("DataNotFound", code.get(new FetchFailedMarker(Reason.DataNotFound, date, date)));
		assertEquals("ParsingFailed", code.get(new FetchFailedMarker(Reason.ParsingFailed, date, date)));
	}

	public void testDatabaseIntegrityTestDetectsCorruption() throws Exception {
		FetchFailedMarker marker = store(Reason.DataNotFound);
		marker.databaseIntegrityTest();

		getReasonCodeField().set(marker, null);
		storeAndCommit(marker);
		reopenDatabase();
		marker = getSingleMarker();

		try {
			marker.databaseIntegrityTest();
			fail("null as reason code should be detected");
		} catch(NullPointerException e) { }

		try {
			marker.getReason();
			fail("null as reason code should be detected");
		} catch(IllegalArgumentException e) { }

		getReasonCodeField().set(marker, "NoSuchReason");
		storeAndCommit(marker);
		reopenDatabase();
		marker = getSingleMarker();

		try {
			marker.databaseIntegrityTest();
			fail("Invalid reason code should be detected");
		} catch(IllegalStateException e) { }

		try {
			marker.getReason();
			fail("Invalid reason code should be detected");
		} catch(IllegalArgumentException e) { }

		// Fixing it must be possible
		marker.setReason(Reason.DataNotFound);
		storeAndCommit(marker);
		reopenDatabase();
		marker = getSingleMarker();
		assertEquals(Reason.DataNotFound, marker.getReason());
		marker.databaseIntegrityTest();
	}

}
