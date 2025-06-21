import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from './components/ui/card';
import { Button } from './components/ui/button';
import { AlertCircle, Lightbulb } from 'lucide-react';

const API_BASE_URL = 'http://localhost:8080/api';

function App() {
  const [wagoStatus, setWagoStatus] = useState(null);
  const [siemensData, setSiemensData] = useState({
    ist: null,
    soll: null,
    differenz: null
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Nur für Lampen (Wago)
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

    pollWago(); // direkt beim Mount
    const interval = setInterval(pollWago, 700); // alle 0,5s

    return () => {
      active = false;
      clearInterval(interval);
    };
  }, []);

  // Nur für Siemens-Werte (langsameres Polling)
  useEffect(() => {
    let active = true;

    const pollSiemens = async () => {
      setLoading(true);
      try {
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
    const interval = setInterval(pollSiemens, 1000); // alle 5s reicht meistens

    return () => {
      active = false;
      clearInterval(interval);
    };
  }, []);

  const sendControlCommand = async (command) => {
    try {
      const response = await fetch(`${API_BASE_URL}/wago/control`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ command })
      });

      if (response.ok) {
        setTimeout(() => {
          // Sofort neu holen nach Schalten!
          fetch(`${API_BASE_URL}/wago/status/latest`)
              .then(res => res.ok ? res.json() : null)
              .then(data => setWagoStatus(data));
        }, 300);
      }
    } catch (err) {
      setError('Fehler beim Senden des Kommandos');
    }
  };

  // Lampen-Logik wie gehabt:
  const getLightStatus = (status) => {
    if (!status && status !== 0) return Array(16).fill(false);
    return Array.from({ length: 16 }, (_, i) => (status & (1 << i)) !== 0);
  };
  const lights = wagoStatus ? getLightStatus(wagoStatus.status) : Array(16).fill(false);

  return (
      <div className="min-h-screen bg-gray-50 p-4">
        <div className="max-w-6xl mx-auto">
          <h1 className="text-3xl font-bold text-center mb-8 text-gray-800">
            SPS Monitoring & Control System
          </h1>
          {error && (
              <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4 flex items-center">
                <AlertCircle className="mr-2" />
                {error}
              </div>
          )}

          {/* Wago 750 Section */}
          <Card className="mb-6">
            <CardHeader>
              <CardTitle className="text-2xl">SPS: Wago 750</CardTitle>
            </CardHeader>
            <CardContent>
              <>
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

          {/* Siemens S7-1500 Section */}
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
                      <div className="text-lg font-semibold">Ist-Wert: {siemensData.ist?.toFixed(2) ?? 'N/A'}</div>
                    </div>
                    <div className="bg-blue-600 text-white p-4 rounded">
                      <div className="text-lg font-semibold">Soll-Wert: {siemensData.soll?.toFixed(2) ?? 'N/A'}</div>
                    </div>
                    <div className="bg-blue-600 text-white p-4 rounded">
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
