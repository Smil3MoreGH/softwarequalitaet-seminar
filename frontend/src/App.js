import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from './components/ui/card';
import { Button } from './components/ui/button';
import { AlertCircle, Lightbulb } from 'lucide-react';

// Hier lege ich die Basis-URL für meine API fest (kann später leicht angepasst werden)
const API_BASE_URL = 'http://localhost:8080/api';

function App() {
  // State für den aktuellen Status der Wago-Lampen
  const [wagoStatus, setWagoStatus] = useState(null);
  // State für die Siemens-Werte
  const [siemensData, setSiemensData] = useState({
    ist: null,
    soll: null,
    differenz: null
  });
  // State für Ladeanzeige und Fehler
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Hier hole ich regelmäßig den aktuellen Lampenstatus von der Wago SPS ab
  useEffect(() => {
    let active = true;

    const pollWago = async () => {
      try {
        const response = await fetch(`${API_BASE_URL}/wago/status/latest`);
        if (response.ok && active) {
          const data = await response.json();
          setWagoStatus(data);
        }
      } catch (err) {
        if (active) setError('Fehler beim Laden des Wago-Status');
      }
    };

    pollWago(); // Gleich beim Start einmal ausführen
    const interval = setInterval(pollWago, 700); // Polling alle 0,7 Sekunden

    // Beim Unmount alles aufräumen
    return () => {
      active = false;
      clearInterval(interval);
    };
  }, []);

  // Hier hole ich regelmäßig die Temperaturwerte von der Siemens SPS (langsamer als bei Wago)
  useEffect(() => {
    let active = true;

    const pollSiemens = async () => {
      setLoading(true);
      try {
        // Ich hole alle drei Werte parallel ab
        const [istRes, sollRes, diffRes] = await Promise.all([
          fetch(`${API_BASE_URL}/siemens/temperatur/ist/latest`),
          fetch(`${API_BASE_URL}/siemens/temperatur/soll/latest`),
          fetch(`${API_BASE_URL}/siemens/temperatur/differenz/latest`)
        ]);

        const ist = istRes.ok ? await istRes.json() : null;
        const soll = sollRes.ok ? await sollRes.json() : null;
        const diff = diffRes.ok ? await diffRes.json() : null;

        if (active) {
          setSiemensData({
            ist: ist?.istTemperatur,
            soll: soll?.sollTemperatur,
            differenz: diff?.differenzTemperatur
          });
          setLoading(false);
        }
      } catch (err) {
        if (active) setError('Fehler beim Laden der Siemens-Daten');
        setLoading(false);
      }
    };

    pollSiemens();
    const interval = setInterval(pollSiemens, 1000); // Polling alle 1 Sekunde

    return () => {
      active = false;
      clearInterval(interval);
    };
  }, []);

  // Hier verschicke ich einen Control-Befehl an die Wago SPS
  const sendControlCommand = async (command) => {
    try {
      const response = await fetch(`${API_BASE_URL}/wago/control`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ command })
      });

      if (response.ok) {
        // Nach dem Senden nochmal sofort aktualisieren
        setTimeout(() => {
          fetch(`${API_BASE_URL}/wago/status/latest`)
              .then(res => res.ok ? res.json() : null)
              .then(data => setWagoStatus(data));
        }, 300);
      }
    } catch (err) {
      setError('Fehler beim Senden des Kommandos');
    }
  };

  // Hier wandle ich den Integer-Status der Wago in ein Array für die 16 Lampen um
  const getLightStatus = (status) => {
    if (!status && status !== 0) return Array(16).fill(false);
    // Bitweise prüfen, ob die Lampe an ist
    return Array.from({ length: 16 }, (_, i) => (status & (1 << i)) !== 0);
  };
  const lights = wagoStatus ? getLightStatus(wagoStatus.status) : Array(16).fill(false);

  // Ab hier folgt mein Frontend-Layout mit Tailwind und shadcn/ui Komponenten
  return (
      <div className="min-h-screen bg-gray-50 p-4">
        <div className="max-w-6xl mx-auto">
          <h1 className="text-3xl font-bold text-center mb-8 text-gray-800">
            SPS Monitoring & Control System
          </h1>
          {/* Fehleranzeige falls Fehler aufgetreten sind */}
          {error && (
              <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4 flex items-center">
                <AlertCircle className="mr-2" />
                {error}
              </div>
          )}

          {/* Wago 750 Bereich */}
          <Card className="mb-6">
            <CardHeader>
              <CardTitle className="text-2xl">SPS: Wago 750</CardTitle>
            </CardHeader>
            <CardContent>
              <>
                {/* Visualisierung der Lampen */}
                <div className="grid grid-cols-8 gap-4 mb-6">
                  {lights.slice(0, 8).map((isOn, index) => (
                      <div key={index} className="flex flex-col items-center">
                        <Lightbulb
                            size={48}
                            className={isOn ? 'text-yellow-500 fill-yellow-500' : 'text-gray-300'}
                        />
                        <span className="text-sm mt-1">L{index + 1}</span>
                      </div>
                  ))}
                </div>
                <div className="grid grid-cols-8 gap-4 mb-6">
                  {lights.slice(8, 16).map((isOn, index) => (
                      <div key={index + 8} className="flex flex-col items-center">
                        <Lightbulb
                            size={48}
                            className={isOn ? 'text-yellow-500 fill-yellow-500' : 'text-gray-300'}
                        />
                        <span className="text-sm mt-1">L{index + 9}</span>
                      </div>
                  ))}
                </div>
                {/* Buttons zum Senden der vier Steuerbefehle */}
                <div className="flex gap-2 justify-center">
                  {[0, 1, 2, 3].map(mode => (
                      <Button
                          key={mode}
                          onClick={() => sendControlCommand(mode)}
                          className="bg-blue-500 hover:bg-blue-600"
                      >
                        SPS Mode {mode}
                      </Button>
                  ))}
                </div>
              </>
            </CardContent>
          </Card>

          {/* Siemens S7-1500 Bereich */}
          <Card>
            <CardHeader>
              <CardTitle className="text-2xl">SPS: S7-1500</CardTitle>
            </CardHeader>
            <CardContent>
              {loading ? (
                  <div className="text-center py-8">Loading...</div>
              ) : (
                  <div className="space-y-4">
                    <div className="bg-blue-600 text-white p-4 rounded">
                      {/* Anzeige Ist-Wert */}
                      <div className="text-lg font-semibold">Ist-Wert: {siemensData.ist?.toFixed(2) ?? 'N/A'}</div>
                    </div>
                    <div className="bg-blue-600 text-white p-4 rounded">
                      {/* Anzeige Soll-Wert */}
                      <div className="text-lg font-semibold">Soll-Wert: {siemensData.soll?.toFixed(2) ?? 'N/A'}</div>
                    </div>
                    <div className="bg-blue-600 text-white p-4 rounded">
                      {/* Anzeige Differenz */}
                      <div className="text-lg font-semibold">Differenz-Wert: {siemensData.differenz?.toFixed(2) ?? 'N/A'}</div>
                    </div>
                  </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
  );
}

export default App;
