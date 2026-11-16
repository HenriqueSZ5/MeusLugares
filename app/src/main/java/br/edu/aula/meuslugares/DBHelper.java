package br.edu.aula.meuslugares;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Cria o banco SQLite e concentra todas as operações CRUD da tabela "lugares".
 * A Activity nunca escreve SQL diretamente.
 */
public class DBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "lugares.db";
    public static final int DATABASE_VERSION = 2; // v2: coluna "endereco"

    public static final String TABLE_LUGARES = "lugares";
    public static final String COL_ID = "id";
    public static final String COL_NOME = "nome";
    public static final String COL_LATITUDE = "latitude";
    public static final String COL_LONGITUDE = "longitude";
    public static final String COL_ENDERECO = "endereco";
    public static final String COL_FOTO = "caminho_foto";
    public static final String COL_DATA = "data_criacao";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE_LUGARES + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NOME + " TEXT NOT NULL, " +
                COL_LATITUDE + " REAL NOT NULL, " +
                COL_LONGITUDE + " REAL NOT NULL, " +
                COL_ENDERECO + " TEXT, " +
                COL_FOTO + " TEXT, " +
                COL_DATA + " TEXT NOT NULL)";
        db.execSQL(sql);
    }

    // Chamado quando DATABASE_VERSION aumenta. Usamos ALTER TABLE para NÃO perder os lugares já salvos.
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_LUGARES + " ADD COLUMN " + COL_ENDERECO + " TEXT");
        }
    }

    // ===================== CREATE =====================
    /** Insere um lugar e devolve o id gerado (ou -1 se der erro). */
    public long inserir(Lugar lugar) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(COL_NOME, lugar.getNome());
        valores.put(COL_LATITUDE, lugar.getLatitude());
        valores.put(COL_LONGITUDE, lugar.getLongitude());
        valores.put(COL_ENDERECO, lugar.getEndereco());
        valores.put(COL_FOTO, lugar.getCaminhoFoto());
        valores.put(COL_DATA, lugar.getDataCriacao());
        return db.insert(TABLE_LUGARES, null, valores);
    }

    // ===================== READ =====================
    /** Lista todos os lugares, do mais recente para o mais antigo. */
    public List<Lugar> listarTodos() {
        List<Lugar> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        // Equivale a: SELECT * FROM lugares ORDER BY id DESC
        Cursor cursor = db.query(TABLE_LUGARES, null, null, null, null, null, COL_ID + " DESC");
        try {
            while (cursor.moveToNext()) {
                lista.add(new Lugar(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_NOME)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LATITUDE)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LONGITUDE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_ENDERECO)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_FOTO)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_DATA))));
            }
        } finally {
            cursor.close(); // SEMPRE feche o Cursor
        }
        return lista;
    }

    // ===================== UPDATE =====================
    /** Altera o nome de um lugar. Devolve quantas linhas foram alteradas. */
    public int atualizarNome(long id, String novoNome) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(COL_NOME, novoNome);
        // O "?" é substituído pelo valor do array: evita SQL Injection
        return db.update(TABLE_LUGARES, valores, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }

    // ===================== DELETE =====================
    /** Exclui um lugar pelo id. Devolve quantas linhas foram excluídas. */
    public int excluir(long id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_LUGARES, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }
}
