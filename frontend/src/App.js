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

  // Fetch data functions
  const fetchWagoStatus = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/wago/status/latest`);
      if (response.ok) {
        const data = await response.json();
        setWagoStatus(data);
      }
    } catch (err) {
      console.error('Error fetching Wago status:', err);
      setError('Failed to fetch Wago status');
    }
  };

  const fetchSiemensData = async () => {
    try {
      const [istRes, sollRes, diffRes] = await Promise.all([
        fetch(`${API_BASE_URL}/siemens/temperatur/ist/latest`),
        fetch(`${API_BASE_URL}/siemens/temperatur/soll/latest`),
        fetch(`${API_BASE_URL}/siemens/temperatur/differenz/latest`)
      ]);

      const ist = istRes.ok ? await istRes.json() : null;
      const soll = sollRes.ok ? await sollRes.json() : null;
      const diff = diffRes.ok ? await diffRes.json() : null;

      setSiemensData({
        ist: ist?.istTemperatur,
        soll: soll?.sollTemperatur,
        differenz: diff?.differenzTemperatur
      });
    } catch (err) {
      console.error('Error fetching Siemens data:', err);
      setError('Failed to fetch Siemens data');
    }
  };

  const sendControlCommand = async (command) => {
    try {
      const response = await fetch(`${API_BASE_URL}/wago/control`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ command })
      });

      if (response.ok) {
        console.log(`Control command ${command} sent successfully`);
        // Refresh status after sending command
        setTimeout(fetchWagoStatus, 1000);
      }
    } catch (err) {
      console.error('Error sending control command:', err);
      setError('Failed to send control command');
    }
  };

  // Initial data fetch and polling
  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      await Promise.all([fetchWagoStatus(), fetchSiemensData()]);
      setLoading(false);
    };

    fetchData();

    // Poll for updates every 5 seconds
    const interval = setInterval(fetchData, 5000);

    return () => clearInterval(interval);
  }, []);

  // Convert status to binary representation for lights
  const getLightStatus = (status) => {
    if (!status) return Array(16).fill(false);

    const binary = status.toString(2).padStart(16, '0');
    return binary.split('').map(bit => bit === '1');
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
              {loading ? (
                  <div className="text-center py-8">Loading...</div>
              ) : (
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
                      <Button
                          onClick={() => sendControlCommand(0)}
                          className="bg-blue-500 hover:bg-blue-600"
                      >
                        SPS Mode 0
                      </Button>
                      <Button
                          onClick={() => sendControlCommand(1)}
                          className="bg-blue-500 hover:bg-blue-600"
                      >
                        SPS Mode 1
                      </Button>
                      <Button
                          onClick={() => sendControlCommand(2)}
                          className="bg-blue-500 hover:bg-blue-600"
                      >
                        SPS Mode 2
                      </Button>
                      <Button
                          onClick={() => sendControlCommand(3)}
                          className="bg-blue-500 hover:bg-blue-600"
                      >
                        SPS Mode 3
                      </Button>
                    </div>
                  </>
              )}
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
                      <div className="text-lg font-semibold">Ist-Wert: {siemensData.ist?.toFixed(15) || 'N/A'}</div>
                    </div>
                    <div className="bg-blue-600 text-white p-4 rounded">
                      <div className="text-lg font-semibold">Soll-Wert: {siemensData.soll?.toFixed(15) || 'N/A'}</div>
                    </div>
                    <div className="bg-blue-600 text-white p-4 rounded">
                      <div className="text-lg font-semibold">Differenz-Wert: {siemensData.differenz?.toFixed(15) || 'N/A'}</div>
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