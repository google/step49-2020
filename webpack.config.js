const path = require("path");

// webpack.config.js
module.exports = {
  mode: 'development',
  entry: './src/main/webapp/script.js',
  output: {
    filename: 'main.js',
    path: path.join(__dirname, '/src/main/webapp/dist')
  }
};