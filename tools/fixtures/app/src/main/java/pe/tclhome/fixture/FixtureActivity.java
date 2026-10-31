package pe.tclhome.fixture;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

/** Emulator-only, independent installed applications. Never shipped inside the launcher. */
public final class FixtureActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        TextView content = new TextView(this);
        content.setGravity(Gravity.CENTER);
        content.setTextColor(0xFFF4F5FA);
        content.setBackgroundColor(0xFF101725);
        content.setTextSize(28);
        content.setText(getString(R.string.app_name) + "\n\nAplicación de prueba\nPulsa Atrás para volver a Inicio");
        setContentView(content);
    }
}
