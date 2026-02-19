package com.dragonminez.server.storage;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.*;
import java.sql.*;
import java.util.UUID;

public class DatabaseManager implements IDataStorage {
	private HikariDataSource dataSource;
	private boolean isConnected = false;

	public DatabaseManager() {}

	@Override
	public void init() {
		GeneralServerConfig.StorageConfig config = ConfigManager.getServerConfig().getStorage();
		if (!hasValidCredentials(config)) {
			LogUtil.error(Env.SERVER, "DATABASE ERROR: Missing credentials (Host, DB Name, User or Password).");
			LogUtil.error(Env.SERVER, "FALLBACK: System will use Default Local NBT Storage.");
			isConnected = false;
			return;
		}

		LogUtil.info(Env.SERVER, "Connecting to Database: " + config.getHost() + ":" + config.getPort());

		HikariConfig hikariConfig = new HikariConfig();
		String jdbcUrl = "jdbc:mariadb://" + config.getHost() + ":" + config.getPort() + "/" + config.getDatabase();

		hikariConfig.setJdbcUrl(jdbcUrl);
		hikariConfig.setUsername(config.getUsername());
		hikariConfig.setPassword(config.getPassword());
		hikariConfig.setMaximumPoolSize(config.getPoolSize());
		hikariConfig.setPoolName("DragonMineZ-Pool");

		hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
		hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
		hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
		hikariConfig.setConnectionTimeout(5000);

		try {
			dataSource = new HikariDataSource(hikariConfig);
			createTable(config.getTable());
			isConnected = true;
			LogUtil.info(Env.SERVER, "Database connected successfully!");
		} catch (Exception e) {
			LogUtil.error(Env.SERVER, "CRITICAL: Failed to connect to database: " + e.getMessage());
			LogUtil.error(Env.SERVER, "FALLBACK: System will use Default Local NBT Storage to prevent data loss.");
			isConnected = false;
		}
	}

	private boolean hasValidCredentials(GeneralServerConfig.StorageConfig config) {
		return !config.getHost().isEmpty() &&
				!config.getDatabase().isEmpty() &&
				!config.getUsername().isEmpty() &&
				!config.getPassword().isEmpty();
	}

	private void createTable(String tableName) {
		String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
				"uuid VARCHAR(36) PRIMARY KEY, " +
				"name VARCHAR(64), " +
				"data MEDIUMBLOB, " +
				"last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
				");";

		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.execute();
		} catch (SQLException e) {
			LogUtil.error(Env.SERVER, "Error creating table: " + e.getMessage());
			isConnected = false;
		}
	}

	@Override
	public boolean saveData(UUID uuid, String name, CompoundTag tag) {
		if (!isConnected || dataSource == null) return false;

		String tableName = ConfigManager.getServerConfig().getStorage().getTable();

		String sql = "INSERT INTO " + tableName + " (uuid, name, data) VALUES (?, ?, ?) " +
				"ON DUPLICATE KEY UPDATE name = ?, data = ?, last_updated = CURRENT_TIMESTAMP";

		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(sql)) {

			byte[] dataBytes = nbtToBytes(tag);

			stmt.setString(1, uuid.toString());
			stmt.setString(2, name);
			stmt.setBytes(3, dataBytes);
			stmt.setString(4, name);
			stmt.setBytes(5, dataBytes);

			stmt.executeUpdate();
			return true;
		} catch (SQLException e) {
			LogUtil.error(Env.SERVER, "Failed to save player " + name + " to DB: " + e.getMessage());
			return false;
		}
	}

	@Override
	public CompoundTag loadData(UUID uuid) {
		if (!isConnected || dataSource == null) return null;

		String tableName = ConfigManager.getServerConfig().getStorage().getTable();
		String sql = "SELECT data FROM " + tableName + " WHERE uuid = ?";

		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, uuid.toString());

			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					try (InputStream is = rs.getBinaryStream("data")) {
						if (is != null) {
							return NbtIo.readCompressed(is);
						}
					} catch (IOException e) {
						LogUtil.error(Env.SERVER, "Error decompressing NBT for " + uuid + ": " + e.getMessage());
					}
				}
			}
		} catch (SQLException e) {
			LogUtil.error(Env.SERVER, "Failed to load player " + uuid + " from DB: " + e.getMessage());
		}
		return null;
	}

	@Override
	public void shutdown() {
		if (dataSource != null && !dataSource.isClosed()) {
			dataSource.close();
			LogUtil.info(Env.SERVER, "Database connection closed.");
		}
	}

	@Override
	public String getName() {
		return "DATABASE (MariaDB/MySQL)";
	}

	private byte[] nbtToBytes(CompoundTag tag) {
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			NbtIo.writeCompressed(tag, outputStream);
			return outputStream.toByteArray();
		} catch (IOException e) {
			LogUtil.error(Env.SERVER, "Error serializing NBT: " + e.getMessage());
			return new byte[0];
		}
	}
}