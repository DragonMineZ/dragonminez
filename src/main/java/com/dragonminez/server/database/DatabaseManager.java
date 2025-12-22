package com.dragonminez.server.database;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.UUID;

public class DatabaseManager {
	private static HikariDataSource dataSource;
	private static boolean isConnected = false;

	public static void init() {
		GeneralServerConfig.StorageConfig config = ConfigManager.getServerConfig().getStorage();

		if (config.getStorageType() != GeneralServerConfig.StorageConfig.StorageType.DATABASE) {
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

		try {
			dataSource = new HikariDataSource(hikariConfig);
			isConnected = true;
			createTable(config.getTable());
			LogUtil.info(Env.SERVER, "Database connected successfully!");
		} catch (Exception e) {
			LogUtil.error(Env.SERVER, "Failed to connect to database: " + e.getMessage());
			isConnected = false;
		}
	}

	private static void createTable(String tableName) {
		String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
				"uuid VARCHAR(36) PRIMARY KEY, " +
				"name VARCHAR(64), " +
				"data MEDIUMTEXT, " +
				"last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
				");";

		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.execute();
		} catch (SQLException e) {
			LogUtil.error(Env.SERVER, "Error creating table: " + e.getMessage());
		}
	}

	public static void savePlayer(UUID uuid, String name, CompoundTag tag) {
		if (!isConnected || dataSource == null) return;

		String tableName = ConfigManager.getServerConfig().getStorage().getTable();
		String dataString = nbtToString(tag);

		String sql = "INSERT INTO " + tableName + " (uuid, name, data) VALUES (?, ?, ?) " +
				"ON DUPLICATE KEY UPDATE name = ?, data = ?, last_updated = CURRENT_TIMESTAMP";

		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, uuid.toString());
			stmt.setString(2, name);
			stmt.setString(3, dataString);
			stmt.setString(4, name);
			stmt.setString(5, dataString);

			stmt.executeUpdate();
		} catch (SQLException e) {
			LogUtil.error(Env.SERVER, "Failed to save player " + name + ": " + e.getMessage());
		}
	}

	public static CompoundTag loadPlayer(UUID uuid) {
		if (!isConnected || dataSource == null) return null;

		String tableName = ConfigManager.getServerConfig().getStorage().getTable();
		String sql = "SELECT data FROM " + tableName + " WHERE uuid = ?";

		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, uuid.toString());

			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					String dataString = rs.getString("data");
					return stringToNbt(dataString);
				}
			}
		} catch (SQLException e) {
			LogUtil.error(Env.SERVER, "Failed to load player " + uuid + ": " + e.getMessage());
		}
		return null;
	}

	public static void close() {
		if (dataSource != null && !dataSource.isClosed()) {
			dataSource.close();
		}
	}

	private static String nbtToString(CompoundTag tag) {
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			NbtIo.writeCompressed(tag, outputStream);
			return Base64.getEncoder().encodeToString(outputStream.toByteArray());
		} catch (IOException e) {
			LogUtil.error(Env.SERVER, "Error serializing NBT: " + e.getMessage());
			return "";
		}
	}

	private static CompoundTag stringToNbt(String str) {
		try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(str))) {
			return NbtIo.readCompressed(inputStream);
		} catch (Exception e) {
			LogUtil.error(Env.SERVER, "Error deserializing NBT: " + e.getMessage());
			return null;
		}
	}
}