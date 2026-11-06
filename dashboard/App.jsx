import React, { useEffect, useState } from 'react';

export default function Dashboard() {
  const [logs, setLogs] = useState([]);

  return (
    <div className="p-6 bg-gray-900 text-white min-h-screen">
      <h1 className="text-2xl font-bold mb-4">Eyes Monitoring Dashboard</h1>
      <p className="text-gray-400">Waiting for telemetry from target device...</p>
    </div>
  );
}
