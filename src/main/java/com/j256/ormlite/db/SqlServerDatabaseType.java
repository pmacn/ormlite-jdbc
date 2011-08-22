package com.j256.ormlite.db;

import java.sql.SQLException;
import java.util.List;

import com.j256.ormlite.field.DataPersister;
import com.j256.ormlite.field.FieldConverter;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.support.DatabaseResults;

/**
 * Microsoft SQL server database type information used to create the tables, etc..
 * 
 * <p>
 * <b>WARNING:</b> I have not tested this unfortunately because of a lack of permanent access to a MSSQL instance.
 * </p>
 * 
 * @author graywatson
 */
public class SqlServerDatabaseType extends BaseDatabaseType implements DatabaseType {

	private final static String DATABASE_URL_PORTION = "sqlserver";
	private final static String DRIVER_CLASS_NAME = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	private final static String DATABASE_NAME = "SQL Server";

	private final static FieldConverter byteConverter = new ByteFieldConverter();
	private final static FieldConverter booleanConverter = new BooleanNumberFieldConverter();

	public boolean isDatabaseUrlThisType(String url, String dbTypePart) {
		return DATABASE_URL_PORTION.equals(dbTypePart);
	}

	@Override
	protected String getDriverClassName() {
		return DRIVER_CLASS_NAME;
	}

	public String getDatabaseName() {
		return DATABASE_NAME;
	}

	@Override
	public FieldConverter getFieldConverter(DataPersister dataType) {
		// we are only overriding certain types
		switch (dataType.getSqlType()) {
			case BOOLEAN :
				return booleanConverter;
			case BYTE :
				return byteConverter;
			default :
				return super.getFieldConverter(dataType);
		}
	}

	@Override
	protected void appendBooleanType(StringBuilder sb, int fieldWidth) {
		sb.append("BIT");
	}

	@Override
	protected void appendByteType(StringBuilder sb, int fieldWidth) {
		// TINYINT exists but it gives 0-255 unsigned
		// http://msdn.microsoft.com/en-us/library/ms187745.aspx
		sb.append("SMALLINT");
	}

	@Override
	protected void appendDateType(StringBuilder sb, int fieldWidth) {
		// TIMESTAMP is some sort of internal database type
		// http://www.sqlteam.com/article/timestamps-vs-datetime-data-types
		sb.append("DATETIME");
	}

	@Override
	protected void appendByteArrayType(StringBuilder sb, int fieldWidth) {
		sb.append("IMAGE");
	}

	@Override
	protected void appendSerializableType(StringBuilder sb, int fieldWidth) {
		sb.append("IMAGE");
	}

	@Override
	protected void configureGeneratedId(String tableName, StringBuilder sb, FieldType fieldType,
			List<String> statementsBefore, List<String> statementsAfter, List<String> additionalArgs,
			List<String> queriesAfter) {
		sb.append("IDENTITY ");
		configureId(sb, fieldType, statementsBefore, additionalArgs, queriesAfter);
		if (fieldType.isAllowGeneratedIdInsert()) {
			StringBuilder identityInsertSb = new StringBuilder();
			identityInsertSb.append("SET IDENTITY_INSERT ");
			appendEscapedEntityName(identityInsertSb, tableName);
			identityInsertSb.append(" ON");
			statementsAfter.add(identityInsertSb.toString());
		}
	}

	@Override
	public void appendEscapedEntityName(StringBuilder sb, String word) {
		sb.append('\"').append(word).append('\"');
	}

	@Override
	public boolean isLimitAfterSelect() {
		return true;
	}

	@Override
	public void appendLimitValue(StringBuilder sb, int limit, Integer offset) {
		sb.append("TOP ").append(limit).append(' ');
	}

	@Override
	public boolean isOffsetSqlSupported() {
		// there is no easy way to do this in this database type
		return false;
	}

	@Override
	public boolean isAllowGeneratedIdInsertSupported() {
		/*
		 * The only way sql-server allows this is if "SET IDENTITY_INSERT table-name ON" has been set. However this is a
		 * runtime session value and not a configuration option. Grrrrr.
		 */
		return false;
	}

	/**
	 * Conversion from the byte Java field to the SMALLINT Jdbc type because TINYINT looks to be 0-255 and unsigned.
	 */
	private static class ByteFieldConverter implements FieldConverter {
		public SqlType getSqlType() {
			// store it as a short
			return SqlType.BYTE;
		}
		public Object parseDefaultString(FieldType fieldType, String defaultStr) {
			return Short.parseShort(defaultStr);
		}
		public Object javaToSqlArg(FieldType fieldType, Object javaObject) {
			// convert the Byte arg to be a short
			byte byteVal = (Byte) javaObject;
			return (short) byteVal;
		}
		public Object resultToJava(FieldType fieldType, DatabaseResults results, int dbColumnPos) throws SQLException {
			// starts as a short and then gets converted to a byte on the way out
			short shortVal = results.getShort(dbColumnPos);
			// make sure the database value doesn't overflow the byte
			if (shortVal < Byte.MIN_VALUE) {
				return Byte.MIN_VALUE;
			} else if (shortVal > Byte.MAX_VALUE) {
				return Byte.MAX_VALUE;
			} else {
				return (byte) shortVal;
			}
		}
		public boolean isStreamType() {
			return false;
		}
	}
}
