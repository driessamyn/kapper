package net.samyn.kapper.benchmark.setup

import java.sql.Connection
import java.util.UUID
import kotlin.math.min

fun Connection.createTables() {
    this.createStatement().execute(
        """
        CREATE TABLE IF NOT EXISTS super_heroes (
            id UUID NOT NULL PRIMARY KEY,
            name VARCHAR(100) NOT NULL,
            email VARCHAR(100) NOT NULL,
            age INT
        );
        """.trimIndent(),
    )
    this.createStatement().execute(
        """
        CREATE TABLE IF NOT EXISTS villains (
            id UUID NOT NULL PRIMARY KEY,
            name VARCHAR(100) NOT NULL
         );
        """.trimIndent(),
    )
    this.createStatement().execute(
        """
        CREATE TABLE IF NOT EXISTS battles (
            super_hero_id UUID NOT NULL,
            villain_id UUID NOT NULL,
            battle_date TIMESTAMP NOT NULL,
            updated_ts TIMESTAMP NOT NULL,
            PRIMARY KEY (super_hero_id, villain_id, battle_date),
        
            CONSTRAINT fk_super_hero FOREIGN KEY (super_hero_id)
                    REFERENCES super_heroes(id),
            CONSTRAINT fk_villains FOREIGN KEY (villain_id)
                    REFERENCES villains(id)
        );
        """.trimIndent(),
    )
}

fun Connection.insertTestData(
    nHeroes: Int,
    nVillains: Int,
) {
    autoCommit = false
    val heroStmt =
        this.prepareStatement(
            """
            INSERT INTO super_heroes (id, name, email, age) VALUES
            (?, ?, ?, ?);
            """,
        )
    (0..nHeroes).forEach {
        heroStmt.setObject(1, heroId(it))
        heroStmt.setString(2, "SuperHero$it")
        heroStmt.setString(3, "hero$it@universe.com")
        heroStmt.setInt(4, it)
        heroStmt.addBatch()
    }
    heroStmt.executeBatch()
    val villainStmt =
        this.prepareStatement(
            """
            INSERT INTO villains (id, name) VALUES
            (?, ?);
            """,
        )
    (0..nVillains).forEach {
        villainStmt.setObject(1, villainId(it))
        villainStmt.setString(2, "Villain$it")
        villainStmt.addBatch()
    }
    villainStmt.executeBatch()
    val battleStmt =
        this.prepareStatement(
            """
            INSERT INTO battles (super_hero_id, villain_id, battle_date, updated_ts) VALUES
            (?, ?, ?, ?);
            """,
        )
    (0..min(nHeroes, nVillains)).forEach {
        battleStmt.setObject(1, heroId(it))
        battleStmt.setObject(2, villainId(it))
        battleStmt.setDate(3, java.sql.Date(it * 1000L * 60 * 60 * 24))
        battleStmt.setTimestamp(4, java.sql.Timestamp(System.currentTimeMillis()))
        battleStmt.addBatch()
    }
    battleStmt.executeBatch()
    commit()
}

fun heroId(i: Int): UUID = UUID.nameUUIDFromBytes("1$i".toByteArray())

fun villainId(i: Int): UUID = UUID.nameUUIDFromBytes("2$i".toByteArray())
