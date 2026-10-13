package com.example.offlineqr

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.offlineqr.data.entity.FolderEntity
import com.example.offlineqr.data.entity.QRCodeEntity
import com.example.offlineqr.data.local.FolderDao
import com.example.offlineqr.data.local.OfflineQrDatabase
import com.example.offlineqr.data.local.QrCodeDao
import com.example.offlineqr.domain.model.QrConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QrDaoTest {

    private lateinit var database: OfflineQrDatabase
    private lateinit var qrCodeDao: QrCodeDao
    private lateinit var folderDao: FolderDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, OfflineQrDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        qrCodeDao = database.qrCodeDao()
        folderDao = database.folderDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `test inserting and querying QR codes by category and favorites`() {
        runBlocking {
            val qr1 = QRCodeEntity(
                content = "https://example.com/1",
                type = "OLUSTURULAN",
                isFavorite = true,
                qrConfig = QrConfig()
            )
            val qr2 = QRCodeEntity(
                content = "https://example.com/2",
                type = "TARANAN",
                isFavorite = false,
                qrConfig = QrConfig()
            )

            val id1 = qrCodeDao.insert(qr1)
            val id2 = qrCodeDao.insert(qr2)

            val all = qrCodeDao.getAllQrCodes().first()
            assertEquals(2, all.size)

            val created = qrCodeDao.getCreatedQrCodes().first()
            assertEquals(1, created.size)
            assertEquals(id1, created[0].id)

            val scanned = qrCodeDao.getScannedQrCodes().first()
            assertEquals(1, scanned.size)
            assertEquals(id2, scanned[0].id)

            val favorites = qrCodeDao.getFavoriteQrCodes().first()
            assertEquals(1, favorites.size)
            assertEquals(id1, favorites[0].id)
        }
    }

    @Test
    fun `CRITICAL TEST - deleting a folder sets folderId to null and preserves QR records`() {
        runBlocking {
            // 1. Create a folder
            val folder = FolderEntity(name = "Work Documents")
            val folderId = folderDao.insert(folder)

            // 2. Insert two QR codes assigned to this folder
            val qrInFolder1 = QRCodeEntity(
                content = "Doc 1",
                type = "OLUSTURULAN",
                folderId = folderId,
                qrConfig = QrConfig()
            )
            val qrInFolder2 = QRCodeEntity(
                content = "Doc 2",
                type = "OLUSTURULAN",
                folderId = folderId,
                qrConfig = QrConfig()
            )
            qrCodeDao.insert(qrInFolder1)
            qrCodeDao.insert(qrInFolder2)

            // Verify they are in the folder
            val inFolderBefore = qrCodeDao.getQrCodesByFolder(folderId).first()
            assertEquals(2, inFolderBefore.size)

            // 3. Delete folder using safe atomic transaction
            folderDao.deleteFolderSafely(folderId)

            // 4. Verify folder is deleted
            val deletedFolder = folderDao.getFolderById(folderId).first()
            assertNull(deletedFolder)

            // 5. Verify the QR codes ARE NOT DELETED!
            val allQrCodesAfter = qrCodeDao.getAllQrCodes().first()
            assertEquals(2, allQrCodesAfter.size)

            // 6. Verify their folderId is now null (uncategorized)
            val uncategorized = qrCodeDao.getUncategorizedQrCodes().first()
            assertEquals(2, uncategorized.size)
            for (qr in uncategorized) {
                assertNull("folderId must be null after folder deletion", qr.folderId)
            }
        }
    }
}
