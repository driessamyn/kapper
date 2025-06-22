package net.samyn.kapper.benchmark.mapper

import java.io.InputStream
import java.io.Reader
import java.math.BigDecimal
import java.net.URL
import java.sql.Array
import java.sql.Blob
import java.sql.Clob
import java.sql.Date
import java.sql.NClob
import java.sql.Ref
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.RowId
import java.sql.SQLWarning
import java.sql.SQLXML
import java.sql.Statement
import java.sql.Time
import java.sql.Timestamp
import java.util.Calendar
import java.util.UUID

class ResultSetStub(
    private val rowCount: Int,
    private val nullableColumns: Set<Int> = emptySet(),
    private val uuidColumns: Set<Int> = emptySet(),
) : ResultSet {
    private var currentRow = 0
    private val random = (System.currentTimeMillis() % 1234).toInt()
    private val name = "Name-$random"

    override fun next(): Boolean {
        return ++currentRow <= rowCount
    }

    override fun getString(columnIndex: Int): String? {
        if (isNull(columnIndex)) return null
        return if (columnIndex in uuidColumns) UUID.randomUUID().toString() else "$name-$columnIndex-$currentRow"
    }

    override fun getInt(columnIndex: Int): Int = if (isNull(columnIndex)) 0 else (columnIndex * 1000 + currentRow + random)

    override fun getBoolean(columnIndex: Int): Boolean = !isNull(columnIndex) && (currentRow + columnIndex + random) % 2 == 0

    override fun getDouble(columnIndex: Int): Double = if (isNull(columnIndex)) 0.0 else (columnIndex * 1.1 + currentRow + random)

    private fun isNull(columnIndex: Int): Boolean = columnIndex in nullableColumns && (currentRow + columnIndex + random) % 2 != 0

    override fun wasNull() = false

    override fun <T : Any?> unwrap(iface: Class<T>?): T {
        throw NotImplementedError()
    }

    override fun isWrapperFor(iface: Class<*>?): Boolean {
        throw NotImplementedError()
    }

    override fun close() = Unit

    override fun getString(columnLabel: String?): String {
        throw NotImplementedError()
    }

    override fun getBoolean(columnLabel: String?): Boolean {
        throw NotImplementedError()
    }

    override fun getByte(columnIndex: Int): Byte {
        throw NotImplementedError()
    }

    override fun getByte(columnLabel: String?): Byte {
        throw NotImplementedError()
    }

    override fun getShort(columnIndex: Int): Short {
        throw NotImplementedError()
    }

    override fun getShort(columnLabel: String?): Short {
        throw NotImplementedError()
    }

    override fun getInt(columnLabel: String?): Int {
        throw NotImplementedError()
    }

    override fun getLong(columnIndex: Int): Long {
        throw NotImplementedError()
    }

    override fun getLong(columnLabel: String?): Long {
        throw NotImplementedError()
    }

    override fun getFloat(columnIndex: Int): Float {
        throw NotImplementedError()
    }

    override fun getFloat(columnLabel: String?): Float {
        throw NotImplementedError()
    }

    override fun getDouble(columnLabel: String?): Double {
        throw NotImplementedError()
    }

    @Deprecated("Deprecated in Java")
    override fun getBigDecimal(
        columnIndex: Int,
        scale: Int,
    ): BigDecimal {
        throw NotImplementedError()
    }

    @Deprecated("Deprecated in Java")
    override fun getBigDecimal(
        columnLabel: String?,
        scale: Int,
    ): BigDecimal {
        throw NotImplementedError()
    }

    override fun getBigDecimal(columnIndex: Int): BigDecimal {
        throw NotImplementedError()
    }

    override fun getBigDecimal(columnLabel: String?): BigDecimal {
        throw NotImplementedError()
    }

    override fun getBytes(columnIndex: Int): ByteArray {
        throw NotImplementedError()
    }

    override fun getBytes(columnLabel: String?): ByteArray {
        throw NotImplementedError()
    }

    override fun getDate(columnIndex: Int): Date {
        throw NotImplementedError()
    }

    override fun getDate(columnLabel: String?): Date {
        throw NotImplementedError()
    }

    override fun getDate(
        columnIndex: Int,
        cal: Calendar?,
    ): Date {
        throw NotImplementedError()
    }

    override fun getDate(
        columnLabel: String?,
        cal: Calendar?,
    ): Date {
        throw NotImplementedError()
    }

    override fun getTime(columnIndex: Int): Time {
        throw NotImplementedError()
    }

    override fun getTime(columnLabel: String?): Time {
        throw NotImplementedError()
    }

    override fun getTime(
        columnIndex: Int,
        cal: Calendar?,
    ): Time {
        throw NotImplementedError()
    }

    override fun getTime(
        columnLabel: String?,
        cal: Calendar?,
    ): Time {
        throw NotImplementedError()
    }

    override fun getTimestamp(columnIndex: Int): Timestamp {
        throw NotImplementedError()
    }

    override fun getTimestamp(columnLabel: String?): Timestamp {
        throw NotImplementedError()
    }

    override fun getTimestamp(
        columnIndex: Int,
        cal: Calendar?,
    ): Timestamp {
        throw NotImplementedError()
    }

    override fun getTimestamp(
        columnLabel: String?,
        cal: Calendar?,
    ): Timestamp {
        throw NotImplementedError()
    }

    override fun getAsciiStream(columnIndex: Int): InputStream {
        throw NotImplementedError()
    }

    override fun getAsciiStream(columnLabel: String?): InputStream {
        throw NotImplementedError()
    }

    @Deprecated("Deprecated in Java")
    override fun getUnicodeStream(columnIndex: Int): InputStream {
        throw NotImplementedError()
    }

    @Deprecated("Deprecated in Java")
    override fun getUnicodeStream(columnLabel: String?): InputStream {
        throw NotImplementedError()
    }

    override fun getBinaryStream(columnIndex: Int): InputStream {
        throw NotImplementedError()
    }

    override fun getBinaryStream(columnLabel: String?): InputStream {
        throw NotImplementedError()
    }

    override fun getWarnings(): SQLWarning {
        throw NotImplementedError()
    }

    override fun clearWarnings() {
        throw NotImplementedError()
    }

    override fun getCursorName(): String {
        throw NotImplementedError()
    }

    override fun getMetaData(): ResultSetMetaData {
        throw NotImplementedError()
    }

    override fun getObject(columnIndex: Int): Any {
        throw NotImplementedError()
    }

    override fun getObject(columnLabel: String?): Any {
        throw NotImplementedError()
    }

    override fun getObject(
        columnIndex: Int,
        map: MutableMap<String, Class<*>>?,
    ): Any {
        throw NotImplementedError()
    }

    override fun getObject(
        columnLabel: String?,
        map: MutableMap<String, Class<*>>?,
    ): Any {
        throw NotImplementedError()
    }

    override fun <T : Any?> getObject(
        columnIndex: Int,
        type: Class<T>?,
    ): T {
        throw NotImplementedError()
    }

    override fun <T : Any?> getObject(
        columnLabel: String?,
        type: Class<T>?,
    ): T {
        throw NotImplementedError()
    }

    override fun findColumn(columnLabel: String?): Int {
        throw NotImplementedError()
    }

    override fun getCharacterStream(columnIndex: Int): Reader {
        throw NotImplementedError()
    }

    override fun getCharacterStream(columnLabel: String?): Reader {
        throw NotImplementedError()
    }

    override fun isBeforeFirst(): Boolean {
        throw NotImplementedError()
    }

    override fun isAfterLast(): Boolean {
        throw NotImplementedError()
    }

    override fun isFirst(): Boolean {
        throw NotImplementedError()
    }

    override fun isLast(): Boolean {
        throw NotImplementedError()
    }

    override fun beforeFirst() {
        throw NotImplementedError()
    }

    override fun afterLast() {
        throw NotImplementedError()
    }

    override fun first(): Boolean {
        throw NotImplementedError()
    }

    override fun last(): Boolean {
        throw NotImplementedError()
    }

    override fun getRow(): Int {
        throw NotImplementedError()
    }

    override fun absolute(row: Int): Boolean {
        throw NotImplementedError()
    }

    override fun relative(rows: Int): Boolean {
        throw NotImplementedError()
    }

    override fun previous(): Boolean {
        throw NotImplementedError()
    }

    override fun setFetchDirection(direction: Int) {
        throw NotImplementedError()
    }

    override fun getFetchDirection(): Int {
        throw NotImplementedError()
    }

    override fun setFetchSize(rows: Int) {
        throw NotImplementedError()
    }

    override fun getFetchSize(): Int {
        throw NotImplementedError()
    }

    override fun getType(): Int {
        throw NotImplementedError()
    }

    override fun getConcurrency(): Int {
        throw NotImplementedError()
    }

    override fun rowUpdated(): Boolean {
        throw NotImplementedError()
    }

    override fun rowInserted(): Boolean {
        throw NotImplementedError()
    }

    override fun rowDeleted(): Boolean {
        throw NotImplementedError()
    }

    override fun updateNull(columnIndex: Int) {
        throw NotImplementedError()
    }

    override fun updateNull(columnLabel: String?) {
        throw NotImplementedError()
    }

    override fun updateBoolean(
        columnIndex: Int,
        x: Boolean,
    ) {
        throw NotImplementedError()
    }

    override fun updateBoolean(
        columnLabel: String?,
        x: Boolean,
    ) {
        throw NotImplementedError()
    }

    override fun updateByte(
        columnIndex: Int,
        x: Byte,
    ) {
        throw NotImplementedError()
    }

    override fun updateByte(
        columnLabel: String?,
        x: Byte,
    ) {
        throw NotImplementedError()
    }

    override fun updateShort(
        columnIndex: Int,
        x: Short,
    ) {
        throw NotImplementedError()
    }

    override fun updateShort(
        columnLabel: String?,
        x: Short,
    ) {
        throw NotImplementedError()
    }

    override fun updateInt(
        columnIndex: Int,
        x: Int,
    ) {
        throw NotImplementedError()
    }

    override fun updateInt(
        columnLabel: String?,
        x: Int,
    ) {
        throw NotImplementedError()
    }

    override fun updateLong(
        columnIndex: Int,
        x: Long,
    ) {
        throw NotImplementedError()
    }

    override fun updateLong(
        columnLabel: String?,
        x: Long,
    ) {
        throw NotImplementedError()
    }

    override fun updateFloat(
        columnIndex: Int,
        x: Float,
    ) {
        throw NotImplementedError()
    }

    override fun updateFloat(
        columnLabel: String?,
        x: Float,
    ) {
        throw NotImplementedError()
    }

    override fun updateDouble(
        columnIndex: Int,
        x: Double,
    ) {
        throw NotImplementedError()
    }

    override fun updateDouble(
        columnLabel: String?,
        x: Double,
    ) {
        throw NotImplementedError()
    }

    override fun updateBigDecimal(
        columnIndex: Int,
        x: BigDecimal?,
    ) {
        throw NotImplementedError()
    }

    override fun updateBigDecimal(
        columnLabel: String?,
        x: BigDecimal?,
    ) {
        throw NotImplementedError()
    }

    override fun updateString(
        columnIndex: Int,
        x: String?,
    ) {
        throw NotImplementedError()
    }

    override fun updateString(
        columnLabel: String?,
        x: String?,
    ) {
        throw NotImplementedError()
    }

    override fun updateBytes(
        columnIndex: Int,
        x: ByteArray?,
    ) {
        throw NotImplementedError()
    }

    override fun updateBytes(
        columnLabel: String?,
        x: ByteArray?,
    ) {
        throw NotImplementedError()
    }

    override fun updateDate(
        columnIndex: Int,
        x: Date?,
    ) {
        throw NotImplementedError()
    }

    override fun updateDate(
        columnLabel: String?,
        x: Date?,
    ) {
        throw NotImplementedError()
    }

    override fun updateTime(
        columnIndex: Int,
        x: Time?,
    ) {
        throw NotImplementedError()
    }

    override fun updateTime(
        columnLabel: String?,
        x: Time?,
    ) {
        throw NotImplementedError()
    }

    override fun updateTimestamp(
        columnIndex: Int,
        x: Timestamp?,
    ) {
        throw NotImplementedError()
    }

    override fun updateTimestamp(
        columnLabel: String?,
        x: Timestamp?,
    ) {
        throw NotImplementedError()
    }

    override fun updateAsciiStream(
        columnIndex: Int,
        x: InputStream?,
        length: Int,
    ) {
        throw NotImplementedError()
    }

    override fun updateAsciiStream(
        columnLabel: String?,
        x: InputStream?,
        length: Int,
    ) {
        throw NotImplementedError()
    }

    override fun updateAsciiStream(
        columnIndex: Int,
        x: InputStream?,
        length: Long,
    ) {
        throw NotImplementedError()
    }

    override fun updateAsciiStream(
        columnLabel: String?,
        x: InputStream?,
        length: Long,
    ) {
        throw NotImplementedError()
    }

    override fun updateAsciiStream(
        columnIndex: Int,
        x: InputStream?,
    ) {
        throw NotImplementedError()
    }

    override fun updateAsciiStream(
        columnLabel: String?,
        x: InputStream?,
    ) {
        throw NotImplementedError()
    }

    override fun updateBinaryStream(
        columnIndex: Int,
        x: InputStream?,
        length: Int,
    ) {
        throw NotImplementedError()
    }

    override fun updateBinaryStream(
        columnLabel: String?,
        x: InputStream?,
        length: Int,
    ) {
        throw NotImplementedError()
    }

    override fun updateBinaryStream(
        columnIndex: Int,
        x: InputStream?,
        length: Long,
    ) {
        throw NotImplementedError()
    }

    override fun updateBinaryStream(
        columnLabel: String?,
        x: InputStream?,
        length: Long,
    ) {
        throw NotImplementedError()
    }

    override fun updateBinaryStream(
        columnIndex: Int,
        x: InputStream?,
    ) {
        throw NotImplementedError()
    }

    override fun updateBinaryStream(
        columnLabel: String?,
        x: InputStream?,
    ) {
        throw NotImplementedError()
    }

    override fun updateCharacterStream(
        columnIndex: Int,
        x: Reader?,
        length: Int,
    ) {
        throw NotImplementedError()
    }

    override fun updateCharacterStream(
        columnLabel: String?,
        reader: Reader?,
        length: Int,
    ) {
        throw NotImplementedError()
    }

    override fun updateCharacterStream(
        columnIndex: Int,
        x: Reader?,
        length: Long,
    ) {
        throw NotImplementedError()
    }

    override fun updateCharacterStream(
        columnLabel: String?,
        reader: Reader?,
        length: Long,
    ) {
        throw NotImplementedError()
    }

    override fun updateCharacterStream(
        columnIndex: Int,
        x: Reader?,
    ) {
        throw NotImplementedError()
    }

    override fun updateCharacterStream(
        columnLabel: String?,
        reader: Reader?,
    ) {
        throw NotImplementedError()
    }

    override fun updateObject(
        columnIndex: Int,
        x: Any?,
        scaleOrLength: Int,
    ) {
        throw NotImplementedError()
    }

    override fun updateObject(
        columnIndex: Int,
        x: Any?,
    ) {
        throw NotImplementedError()
    }

    override fun updateObject(
        columnLabel: String?,
        x: Any?,
        scaleOrLength: Int,
    ) {
        throw NotImplementedError()
    }

    override fun updateObject(
        columnLabel: String?,
        x: Any?,
    ) {
        throw NotImplementedError()
    }

    override fun insertRow() {
        throw NotImplementedError()
    }

    override fun updateRow() {
        throw NotImplementedError()
    }

    override fun deleteRow() {
        throw NotImplementedError()
    }

    override fun refreshRow() {
        throw NotImplementedError()
    }

    override fun cancelRowUpdates() {
        throw NotImplementedError()
    }

    override fun moveToInsertRow() {
        throw NotImplementedError()
    }

    override fun moveToCurrentRow() {
        throw NotImplementedError()
    }

    override fun getStatement(): Statement {
        throw NotImplementedError()
    }

    override fun getRef(columnIndex: Int): Ref {
        throw NotImplementedError()
    }

    override fun getRef(columnLabel: String?): Ref {
        throw NotImplementedError()
    }

    override fun getBlob(columnIndex: Int): Blob {
        throw NotImplementedError()
    }

    override fun getBlob(columnLabel: String?): Blob {
        throw NotImplementedError()
    }

    override fun getClob(columnIndex: Int): Clob {
        throw NotImplementedError()
    }

    override fun getClob(columnLabel: String?): Clob {
        throw NotImplementedError()
    }

    override fun getArray(columnIndex: Int): Array {
        throw NotImplementedError()
    }

    override fun getArray(columnLabel: String?): Array {
        throw NotImplementedError()
    }

    override fun getURL(columnIndex: Int): URL {
        throw NotImplementedError()
    }

    override fun getURL(columnLabel: String?): URL {
        throw NotImplementedError()
    }

    override fun updateRef(
        columnIndex: Int,
        x: Ref?,
    ) {
        throw NotImplementedError()
    }

    override fun updateRef(
        columnLabel: String?,
        x: Ref?,
    ) {
        throw NotImplementedError()
    }

    override fun updateBlob(
        columnIndex: Int,
        x: Blob?,
    ) {
        throw NotImplementedError()
    }

    override fun updateBlob(
        columnLabel: String?,
        x: Blob?,
    ) {
        throw NotImplementedError()
    }

    override fun updateBlob(
        columnIndex: Int,
        inputStream: InputStream?,
        length: Long,
    ) {
        throw NotImplementedError()
    }

    override fun updateBlob(
        columnLabel: String?,
        inputStream: InputStream?,
        length: Long,
    ) {
        throw NotImplementedError()
    }

    override fun updateBlob(
        columnIndex: Int,
        inputStream: InputStream?,
    ) {
        throw NotImplementedError()
    }

    override fun updateBlob(
        columnLabel: String?,
        inputStream: InputStream?,
    ) {
        throw NotImplementedError()
    }

    override fun updateClob(
        columnIndex: Int,
        x: Clob?,
    ) {
        throw NotImplementedError()
    }

    override fun updateClob(
        columnLabel: String?,
        x: Clob?,
    ) {
        throw NotImplementedError()
    }

    override fun updateClob(
        columnIndex: Int,
        reader: Reader?,
        length: Long,
    ) {
        throw NotImplementedError()
    }

    override fun updateClob(
        columnLabel: String?,
        reader: Reader?,
        length: Long,
    ) {
        throw NotImplementedError()
    }

    override fun updateClob(
        columnIndex: Int,
        reader: Reader?,
    ) {
        throw NotImplementedError()
    }

    override fun updateClob(
        columnLabel: String?,
        reader: Reader?,
    ) {
        throw NotImplementedError()
    }

    override fun updateArray(
        columnIndex: Int,
        x: Array?,
    ) {
        throw NotImplementedError()
    }

    override fun updateArray(
        columnLabel: String?,
        x: Array?,
    ) {
        throw NotImplementedError()
    }

    override fun getRowId(columnIndex: Int): RowId {
        throw NotImplementedError()
    }

    override fun getRowId(columnLabel: String?): RowId {
        throw NotImplementedError()
    }

    override fun updateRowId(
        columnIndex: Int,
        x: RowId?,
    ) {
        throw NotImplementedError()
    }

    override fun updateRowId(
        columnLabel: String?,
        x: RowId?,
    ) {
        throw NotImplementedError()
    }

    override fun getHoldability(): Int {
        throw NotImplementedError()
    }

    override fun isClosed(): Boolean {
        throw NotImplementedError()
    }

    override fun updateNString(
        columnIndex: Int,
        nString: String?,
    ) {
        throw NotImplementedError()
    }

    override fun updateNString(
        columnLabel: String?,
        nString: String?,
    ) {
        throw NotImplementedError()
    }

    override fun updateNClob(
        columnIndex: Int,
        nClob: NClob?,
    ) {
        throw NotImplementedError()
    }

    override fun updateNClob(
        columnLabel: String?,
        nClob: NClob?,
    ) {
        throw NotImplementedError()
    }

    override fun updateNClob(
        columnIndex: Int,
        reader: Reader?,
        length: Long,
    ) {
        throw NotImplementedError()
    }

    override fun updateNClob(
        columnLabel: String?,
        reader: Reader?,
        length: Long,
    ) {
        throw NotImplementedError()
    }

    override fun updateNClob(
        columnIndex: Int,
        reader: Reader?,
    ) {
        throw NotImplementedError()
    }

    override fun updateNClob(
        columnLabel: String?,
        reader: Reader?,
    ) {
        throw NotImplementedError()
    }

    override fun getNClob(columnIndex: Int): NClob {
        throw NotImplementedError()
    }

    override fun getNClob(columnLabel: String?): NClob {
        throw NotImplementedError()
    }

    override fun getSQLXML(columnIndex: Int): SQLXML {
        throw NotImplementedError()
    }

    override fun getSQLXML(columnLabel: String?): SQLXML {
        throw NotImplementedError()
    }

    override fun updateSQLXML(
        columnIndex: Int,
        xmlObject: SQLXML?,
    ) {
        throw NotImplementedError()
    }

    override fun updateSQLXML(
        columnLabel: String?,
        xmlObject: SQLXML?,
    ) {
        throw NotImplementedError()
    }

    override fun getNString(columnIndex: Int): String {
        throw NotImplementedError()
    }

    override fun getNString(columnLabel: String?): String {
        throw NotImplementedError()
    }

    override fun getNCharacterStream(columnIndex: Int): Reader {
        throw NotImplementedError()
    }

    override fun getNCharacterStream(columnLabel: String?): Reader {
        throw NotImplementedError()
    }

    override fun updateNCharacterStream(
        columnIndex: Int,
        x: Reader?,
        length: Long,
    ) {
        throw NotImplementedError()
    }

    override fun updateNCharacterStream(
        columnLabel: String?,
        reader: Reader?,
        length: Long,
    ) {
        throw NotImplementedError()
    }

    override fun updateNCharacterStream(
        columnIndex: Int,
        x: Reader?,
    ) {
        throw NotImplementedError()
    }

    override fun updateNCharacterStream(
        columnLabel: String?,
        reader: Reader?,
    ) {
        throw NotImplementedError()
    }
}
