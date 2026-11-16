package br.edu.aula.meuslugares;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.location.LocationManagerCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // Centro inicial do mapa quando ainda não há lugares salvos (Salvador)
    private static final GeoPoint CENTRO_PADRAO = new GeoPoint(-12.9714, -38.5014);

    private MapView mapa;
    private ExtendedFloatingActionButton btnSalvarLocal;
    private DBHelper dbHelper;

    // ---------- Activity Result API ----------

    // 1) Pede FINE e COARSE juntas: desde o Android 12 o usuário pode escolher "aproximada"
    private final ActivityResultLauncher<String[]> pedirLocalizacaoLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), resultado -> {
                boolean precisa = Boolean.TRUE.equals(resultado.get(Manifest.permission.ACCESS_FINE_LOCATION));
                boolean aproximada = Boolean.TRUE.equals(resultado.get(Manifest.permission.ACCESS_COARSE_LOCATION));

                if (precisa || aproximada) {
                    obterLocalizacao();
                } else if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    // Negada e o sistema não mostra mais o diálogo: negada de vez
                    Permissoes.mostrarDialogoConfiguracoes(this,
                            "Para salvar lugares, ative a permissão de localização nas Configurações do app.");
                } else {
                    Toast.makeText(this, "Sem a localização não dá para salvar o lugar", Toast.LENGTH_SHORT).show();
                }
            });

    // 2) Abre o formulário do novo lugar; ao voltar com RESULT_OK, recarrega os pins
    private final ActivityResultLauncher<Intent> novoLugarLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), resultado -> {
                if (resultado.getResultCode() == RESULT_OK) {
                    carregarLugares(true);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // O osmdroid precisa ser configurado ANTES de inflar o MapView
        configurarOsmdroid();

        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        dbHelper = new DBHelper(this);

        mapa = findViewById(R.id.mapa);
        mapa.setTileSource(TileSourceFactory.MAPNIK);
        mapa.setMultiTouchControls(true); // zoom com pinça
        mapa.getController().setZoom(13.0);
        mapa.getController().setCenter(CENTRO_PADRAO);

        btnSalvarLocal = findViewById(R.id.btnSalvarLocal);
        btnSalvarLocal.setOnClickListener(v -> verificarPermissaoLocalizacao());

        carregarLugares(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapa.onResume(); // obrigatório no osmdroid
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapa.onPause(); // obrigatório no osmdroid
    }

    /**
     * O servidor de mapas do OpenStreetMap exige um "User-Agent" que identifique o app.
     * O cache dos mapas fica na pasta privada do app, sem precisar de permissão de armazenamento.
     */
    private void configurarOsmdroid() {
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", Context.MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(getPackageName());
        File base = new File(getCacheDir(), "osmdroid");
        Configuration.getInstance().setOsmdroidBasePath(base);
        Configuration.getInstance().setOsmdroidTileCache(new File(base, "tiles"));
    }

    // ==================== PERMISSÃO DE LOCALIZAÇÃO ====================

    private boolean tem(String permissao) {
        return ContextCompat.checkSelfPermission(this, permissao) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean temLocalizacao() {
        return tem(Manifest.permission.ACCESS_FINE_LOCATION) || tem(Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    /** Decide o que fazer quando o usuário toca em "Salvar local". */
    private void verificarPermissaoLocalizacao() {
        if (temLocalizacao()) {
            obterLocalizacao();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                || shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            Permissoes.mostrarExplicacao(this, "Precisamos da sua localização",
                    "A localização é usada apenas para marcar no mapa o lugar que você está salvando.",
                    this::pedirLocalizacao);
        } else {
            pedirLocalizacao();
        }
    }

    private void pedirLocalizacao() {
        pedirLocalizacaoLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION});
    }

    // ==================== GPS ====================

    /** Pede UMA leitura da posição atual ao LocationManager do Android. */
    private void obterLocalizacao() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!LocationManagerCompat.isLocationEnabled(lm)) {
            new AlertDialog.Builder(this)
                    .setTitle("Localização desligada")
                    .setMessage("Ative a localização do aparelho para salvar o lugar.")
                    .setPositiveButton("Abrir Configurações", (d, w) ->
                            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }

        boolean precisa = tem(Manifest.permission.ACCESS_FINE_LOCATION);
        String provedor = escolherProvedor(lm, precisa);
        if (provedor == null) {
            Toast.makeText(this, "Nenhum provedor de localização disponível", Toast.LENGTH_LONG).show();
            return;
        }

        mostrarBuscando(true);
        try {
            LocationManagerCompat.getCurrentLocation(lm, provedor, (android.os.CancellationSignal) null,
                    ContextCompat.getMainExecutor(this), local -> {
                        mostrarBuscando(false);
                        if (local == null) local = ultimaPosicaoConhecida(lm, provedor);
                        if (local == null) {
                            Toast.makeText(this, "Não foi possível obter a localização. Tente de novo.",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        abrirFormulario(local, !precisa);
                    });
        } catch (SecurityException e) {
            // A permissão foi revogada entre a verificação e o pedido
            mostrarBuscando(false);
            Toast.makeText(this, "Permissão de localização não concedida", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Com localização precisa, o GPS é o mais exato. Com só a aproximada, usamos a rede
     * (no Android 12+ o GPS também funciona, e o sistema arredonda a posição).
     */
    private String escolherProvedor(LocationManager lm, boolean precisa) {
        List<String> candidatos = new ArrayList<>();
        if (precisa) {
            candidatos.add(LocationManager.GPS_PROVIDER);
            candidatos.add(LocationManager.NETWORK_PROVIDER);
        } else {
            candidatos.add(LocationManager.NETWORK_PROVIDER);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                candidatos.add(LocationManager.GPS_PROVIDER);
            }
        }

        for (String p : candidatos) {
            if (lm.isProviderEnabled(p)) return p;
        }
        return null;
    }

    private Location ultimaPosicaoConhecida(LocationManager lm, String provedor) {
        try {
            return lm.getLastKnownLocation(provedor);
        } catch (SecurityException e) {
            return null;
        }
    }

    private void mostrarBuscando(boolean buscando) {
        btnSalvarLocal.setEnabled(!buscando);
        btnSalvarLocal.setText(buscando ? "Obtendo localização..." : "Salvar local");
    }

    private void abrirFormulario(Location local, boolean aproximada) {
        Intent intent = new Intent(this, LugarFormActivity.class);
        intent.putExtra(LugarFormActivity.EXTRA_LATITUDE, local.getLatitude());
        intent.putExtra(LugarFormActivity.EXTRA_LONGITUDE, local.getLongitude());
        intent.putExtra(LugarFormActivity.EXTRA_APROXIMADA, aproximada);
        novoLugarLauncher.launch(intent);
    }

    // ==================== MAPA (READ) ====================

    /** Lê todos os lugares do banco e desenha um pin para cada um. */
    private void carregarLugares(boolean centralizarNoMaisRecente) {
        List<Lugar> lugares = dbHelper.listarTodos(); // READ

        mapa.getOverlays().clear();
        for (Lugar lugar : lugares) {
            Marker pin = new Marker(mapa);
            pin.setPosition(new GeoPoint(lugar.getLatitude(), lugar.getLongitude()));
            pin.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            pin.setTitle(lugar.getNome());
            pin.setOnMarkerClickListener((marcador, mapView) -> {
                mostrarDetalhes(lugar);
                return true;
            });
            mapa.getOverlays().add(pin);
        }
        mapa.invalidate();

        // A lista vem do mais recente para o mais antigo
        if (centralizarNoMaisRecente && !lugares.isEmpty()) {
            Lugar recente = lugares.get(0);
            mapa.getController().animateTo(new GeoPoint(recente.getLatitude(), recente.getLongitude()));
        }
    }

    // ==================== DETALHES, UPDATE E DELETE ====================

    private void mostrarDetalhes(Lugar lugar) {
        View view = getLayoutInflater().inflate(R.layout.dialog_lugar, null);
        ImageView imgFoto = view.findViewById(R.id.imgFoto);
        TextView txtInfo = view.findViewById(R.id.txtInfo);

        if (lugar.temFoto()) {
            imgFoto.setImageBitmap(Imagens.carregarReduzida(lugar.getCaminhoFoto(), 600));
            imgFoto.setOnClickListener(v -> abrirFoto(lugar));
        } else {
            imgFoto.setVisibility(View.GONE);
        }
        String endereco = lugar.temEndereco() ? lugar.getEndereco() + "\n\n" : "";
        txtInfo.setText(String.format(Locale.getDefault(), "%s%s\nLat: %.6f\nLng: %.6f",
                endereco, lugar.getDataCriacao(), lugar.getLatitude(), lugar.getLongitude()));

        new AlertDialog.Builder(this)
                .setTitle(lugar.getNome())
                .setView(view)
                .setPositiveButton("Fechar", null)
                .setNegativeButton("Renomear", (d, w) -> renomear(lugar))
                .setNeutralButton("Excluir", (d, w) -> confirmarExclusao(lugar))
                .show();
    }

    private void renomear(Lugar lugar) {
        EditText campo = new EditText(this);
        campo.setText(lugar.getNome());

        new AlertDialog.Builder(this)
                .setTitle("Renomear lugar")
                .setView(campo)
                .setPositiveButton("Salvar", (d, w) -> {
                    String nome = campo.getText().toString().trim();
                    if (nome.isEmpty()) {
                        Toast.makeText(this, "O nome não pode ficar vazio", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    dbHelper.atualizarNome(lugar.getId(), nome); // UPDATE
                    carregarLugares(false);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmarExclusao(Lugar lugar) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir")
                .setMessage("Excluir \"" + lugar.getNome() + "\" e a foto?")
                .setPositiveButton("Excluir", (d, w) -> {
                    dbHelper.excluir(lugar.getId()); // DELETE
                    if (lugar.temFoto()) {
                        File foto = new File(lugar.getCaminhoFoto());
                        if (foto.exists()) foto.delete();
                    }
                    carregarLugares(false);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /** Abre a foto em tamanho real em outro app (galeria), como na aula de SQLite. */
    private void abrirFoto(Lugar lugar) {
        File arquivo = new File(lugar.getCaminhoFoto());
        if (!arquivo.exists()) {
            Toast.makeText(this, "Foto não encontrada", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", arquivo),
                "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Nenhum app para abrir a foto", Toast.LENGTH_SHORT).show();
        }
    }
}
