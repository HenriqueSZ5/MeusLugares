package br.edu.aula.meuslugares;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/** Banco SQLite e o CRUD da tabela "lugares". */
public class DBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "lugares.db";
    // v2: coluna "endereco". v3: coluna "precisao" e CHECK nas coordenadas
    public static final int DATABASE_VERSION = 3;

    public static final String TABLE_LUGARES = "lugares";
    public static final String COL_ID = "id";
    public static final String COL_NOME = "nome";
    public static final String COL_LATITUDE = "latitude";
    public static final String COL_LONGITUDE = "longitude";
    public static final String COL_PRECISAO = "precisao"; // raio de incerteza do GPS, em metros
    public static final String COL_ENDERECO = "endereco";
    public static final String COL_FOTO = "caminho_foto";
    public static final String COL_DATA = "data_criacao";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(sqlCriarTabela(TABLE_LUGARES));
    }

    // O CHECK barra coordenada impossível, mas não lat/lng trocadas quando as duas cabem no intervalo
    private static String sqlCriarTabela(String nomeTabela) {
        return "CREATE TABLE " + nomeTabela + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NOME + " TEXT NOT NULL, " +
                COL_LATITUDE + " REAL NOT NULL CHECK (" + COL_LATITUDE + " BETWEEN -90 AND 90), " +
                COL_LONGITUDE + " REAL NOT NULL CHECK (" + COL_LONGITUDE + " BETWEEN -180 AND 180), " +
                COL_PRECISAO + " REAL CHECK (" + COL_PRECISAO + " >= 0), " +
                COL_ENDERECO + " TEXT, " +
                COL_FOTO + " TEXT, " +
                COL_DATA + " TEXT NOT NULL)";
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_LUGARES + " ADD COLUMN " + COL_ENDERECO + " TEXT");
        }
        if (oldVersion < 3) {
            // SQLite não tem ADD CONSTRAINT, então recria a tabela e copia os dados
            String colunas = COL_ID + ", " + COL_NOME + ", " + COL_LATITUDE + ", " + COL_LONGITUDE + ", "
                    + COL_ENDERECO + ", " + COL_FOTO + ", " + COL_DATA;
            db.execSQL(sqlCriarTabela("lugares_v3"));
            db.execSQL("INSERT INTO lugares_v3 (" + colunas + ") SELECT " + colunas + " FROM " + TABLE_LUGARES);
            db.execSQL("DROP TABLE " + TABLE_LUGARES);
            db.execSQL("ALTER TABLE lugares_v3 RENAME TO " + TABLE_LUGARES);
        }
    }

    /** Devolve o id gerado, ou -1 se falhar. */
    public long inserir(Lugar lugar) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(COL_NOME, lugar.getNome());
        valores.put(COL_LATITUDE, lugar.getLatitude());
        valores.put(COL_LONGITUDE, lugar.getLongitude());
        valores.put(COL_PRECISAO, lugar.getPrecisao());
        valores.put(COL_ENDERECO, lugar.getEndereco());
        valores.put(COL_FOTO, lugar.getCaminhoFoto());
        valores.put(COL_DATA, lugar.getDataCriacao());
        return db.insert(TABLE_LUGARES, null, valores);
    }

    /** Mais recente primeiro. */
    public List<Lugar> listarTodos() {
        List<Lugar> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(TABLE_LUGARES, null, null, null, null, null, COL_ID + " DESC");
        try {
            int colPrecisao = cursor.getColumnIndexOrThrow(COL_PRECISAO);
            while (cursor.moveToNext()) {
                Double precisao = cursor.isNull(colPrecisao) ? null : cursor.getDouble(colPrecisao);
                lista.add(new Lugar(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_NOME)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LATITUDE)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LONGITUDE)),
                        precisao,
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_ENDERECO)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_FOTO)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_DATA))));
            }
        } finally {
            cursor.close();
        }
        return lista;
    }

    public int atualizarNome(long id, String novoNome) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(COL_NOME, novoNome);
        return db.update(TABLE_LUGARES, valores, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public int excluir(long id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_LUGARES, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }
}
