import React, { useEffect, useState } from 'react';
import { createClient } from '@supabase/supabase-js';

const SUPABASE_URL = 'YOUR_SUPABASE_URL';
const SUPABASE_ANON_KEY = 'YOUR_SUPABASE_ANON_KEY';
const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

export default function Dashboard() {
  const [logs, setLogs] = useState([]);
  const [commands, setCommands] = useState([]);
  const [selectedDevice, setSelectedDevice] = useState('luna_device_01');

  useEffect(() => {
    fetchLogs();
    const subscription = supabase
      .channel('eyes_logs_changes')
      .on('postgres_changes', { event: 'INSERT', schema: 'public', table: 'eyes_logs' }, payload => {
        setLogs(prev => [payload.new, ...prev]);
      })
      .subscribe();

    return () => {
      supabase.removeChannel(subscription);
    };
  }, []);

  async function fetchLogs() {
    const { data, error } = await supabase
      .from('eyes_logs')
      .select('*')
      .order('created_at', { ascending: false })
      .limit(50);
    if (!error && data) setLogs(data);
  }

  async function sendCommand(action) {
    const { error } = await supabase
      .from('eyes_commands')
      .insert([{ device_id: selectedDevice, command: action, status: 'pending' }]);
    if (error) alert('Failed to send command: ' + error.message);
    else alert('Command sent: ' + action);
  }

  return (
    <div className="p-8 bg-gray-950 text-white min-h-screen font-sans">
      <div className="max-w-6xl mx-auto">
        <div className="flex justify-between items-center mb-8 border-b border-gray-800 pb-4">
          <h1 className="text-3xl font-extrabold tracking-tight text-emerald-400">Eyes // Telemetry Dashboard</h1>
          <div className="flex gap-4">
            <button onClick={() => sendCommand('capture_photo')} className="bg-emerald-600 hover:bg-emerald-500 px-4 py-2 rounded text-sm font-semibold transition">Capture Photo</button>
            <button onClick={() => sendCommand('record_audio')} className="bg-amber-600 hover:bg-amber-500 px-4 py-2 rounded text-sm font-semibold transition">Record Audio</button>
            <button onClick={() => sendCommand('panic_alert')} className="bg-rose-600 hover:bg-rose-500 px-4 py-2 rounded text-sm font-semibold transition">Panic Trigger</button>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <div className="bg-gray-900 border border-gray-800 p-5 rounded-xl shadow">
            <h3 className="text-gray-400 text-sm font-medium">Target Device</h3>
            <p className="text-xl font-bold mt-2 text-emerald-300">{selectedDevice}</p>
          </div>
          <div className="bg-gray-900 border border-gray-800 p-5 rounded-xl shadow">
            <h3 className="text-gray-400 text-sm font-medium">Last Battery Level</h3>
            <p className="text-xl font-bold mt-2">{logs[0]?.battery_level ?? '--'}% {logs[0]?.is_charging ? '(Charging)' : ''}</p>
          </div>
          <div className="bg-gray-900 border border-gray-800 p-5 rounded-xl shadow">
            <h3 className="text-gray-400 text-sm font-medium">Last GPS Coordinates</h3>
            <p className="text-sm font-mono mt-3 text-gray-300">{logs[0] ? `${logs[0].latitude}, ${logs[0].longitude}` : 'No data yet'}</p>
          </div>
        </div>

        <div className="bg-gray-900 border border-gray-800 rounded-xl overflow-hidden shadow">
          <div className="px-6 py-4 border-b border-gray-800 font-semibold text-lg">Telemetry History Log</div>
          <div className="divide-y divide-gray-800 max-h-96 overflow-y-auto">
            {logs.map((log) => (
              <div key={log.id} className="px-6 py-4 flex justify-between items-center text-sm">
                <div>
                  <span className="font-mono text-emerald-400">[{log.device_id}]</span> 
                  <span className="ml-3 text-gray-300">Lat: {log.latitude}, Lon: {log.longitude}</span>
                  <span className="ml-4 text-gray-400">Battery: {log.battery_level}%</span>
                </div>
                <div className="text-xs text-gray-500">{new Date(log.created_at).toLocaleString()}</div>
              </div>
            ))}
            {logs.length === 0 && (
              <div className="px-6 py-8 text-center text-gray-500">No logs recorded yet.</div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
