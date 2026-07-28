const http = require('http');

async function testApi() {
  const loginPayload = JSON.stringify({ email: "test2@example.com", password: "Password123!" });
  
  const req = http.request({
    hostname: 'localhost',
    port: 8080,
    path: '/api/auth/login',
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Content-Length': Buffer.byteLength(loginPayload)
    }
  }, (res) => {
    let data = '';
    res.on('data', chunk => data += chunk);
    res.on('end', () => {
      console.log('Login status:', res.statusCode);
      console.log('Login response:', data);
      
      const responseJson = JSON.parse(data);
      const token = responseJson.accessToken;
      if (!token) {
        console.log("No token received, exiting.");
        return;
      }
      
      console.log("Token received, calling /api/dashboard/summary...");
      const dashReq = http.request({
        hostname: 'localhost',
        port: 8080,
        path: '/api/dashboard/summary',
        method: 'GET',
        headers: {
          'Authorization': 'Bearer ' + token
        }
      }, (dashRes) => {
        let dashData = '';
        dashRes.on('data', chunk => dashData += chunk);
        dashRes.on('end', () => {
          console.log('Dashboard status:', dashRes.statusCode);
          console.log('Dashboard response:', dashData.substring(0, 500));
        });
      });
      dashReq.end();
      
      console.log("Calling /api/categories...");
      const catReq = http.request({
        hostname: 'localhost',
        port: 8080,
        path: '/api/categories',
        method: 'GET',
        headers: {
          'Authorization': 'Bearer ' + token
        }
      }, (catRes) => {
        let catData = '';
        catRes.on('data', chunk => catData += chunk);
        catRes.on('end', () => {
          console.log('Categories status:', catRes.statusCode);
          console.log('Categories response:', catData.substring(0, 500));
        });
      });
      catReq.end();
    });
  });
  
  req.write(loginPayload);
  req.end();
}

testApi();
