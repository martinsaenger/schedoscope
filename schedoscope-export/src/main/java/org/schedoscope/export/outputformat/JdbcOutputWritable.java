package org.schedoscope.export.outputformat;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.lib.db.DBWritable;

public class JdbcOutputWritable implements Writable, DBWritable {

	private static final Log LOG = LogFactory.getLog(JdbcOutputWritable.class);

	private String[] line;
	private String[] types;
	private Map<String, String> preparedStatementTypeMapping;

	private static final String STRING = "string";
	private static final String DOUBLE = "double";
	private static final String BOOLEAN = "boolean";
	private static final String INTEGER = "int";
	private static final String LONG = "long";

	public JdbcOutputWritable() {

	};

	public JdbcOutputWritable(String[] line, String[] types,
			Map<String, String> preparedStatementTypeMapping) {
		this.line = line;
		this.types = types;
		this.preparedStatementTypeMapping = preparedStatementTypeMapping;
	}

	@Override
	public void write(PreparedStatement ps) throws SQLException {

		try {
			for (int i = 0; i < line.length; i++) {
				String type = preparedStatementTypeMapping.get(types[i]
						.toLowerCase());

				if (type.equals(JdbcOutputWritable.STRING)) {
					if (!line[i].equals("NULL")) {
						ps.setString(i + 1, line[i]);
					} else {
						ps.setNull(i + 1, Types.VARCHAR);
					}

				} else if (type.equals(JdbcOutputWritable.DOUBLE)) {
					if (!line[i].equals("NULL")) {
						ps.setDouble(i + 1, Double.valueOf(line[i]));
					} else {
						ps.setNull(i + 1, Types.DOUBLE);
					}
				} else if (type.equals(JdbcOutputWritable.BOOLEAN)) {
					if (!line[i].equals("NULL")) {
						ps.setBoolean(i + 1, Boolean.valueOf(line[i]));
					} else {
						ps.setNull(i + 1, Types.BOOLEAN);
					}
				} else if (type.equals(JdbcOutputWritable.INTEGER)) {
					if (!line[i].equals("NULL")) {
						ps.setInt(i + 1, Integer.valueOf(line[i]));
					} else {
						ps.setNull(i + 1, Types.INTEGER);
					}
				} else if (type.equals(JdbcOutputWritable.LONG)) {
					if (!line[i].equals("NULL")) {
						ps.setLong(i + 1, Long.valueOf(line[i]));
					} else {
						ps.setNull(i + 1, Types.BIGINT);
					}

				} else {
					LOG.warn("Unknown column type: " + types[i].toLowerCase());
					ps.setString(i + 1, line[i]);
				}
			}
		} catch (NumberFormatException n) {
			n.printStackTrace();
		}

	}

	@Override
	public void readFields(ResultSet resultSet) throws SQLException {
	}

	@Override
	public void write(DataOutput out) throws IOException {
	}

	@Override
	public void readFields(DataInput in) throws IOException {

	}

}