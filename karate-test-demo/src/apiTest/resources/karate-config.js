function fn() {
  return {
    baseUrl: java.lang.System.getProperty('app.baseUrl', 'http://localhost:8083')
  };
}