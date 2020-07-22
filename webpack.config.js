const path = require("path");

// webpack.config.js
module.exports = {
  mode: 'development',
  entry: './src/main/webapp/script.js',
  output: {
    filename: 'main.js',
    path: path.join(__dirname, '/src/main/webapp/dist'),
    libraryTarget: 'var',
    library: 'graph'
  },
  module: {
    rules: [
      { test: /\.css$/, loader: "style-loader!css-loader" },
      {
        test: /\.s[ac]ss$/i,
        use: [
          // Creates `style` nodes from JS strings
          'style-loader',
          // Translates CSS into CommonJS
          'css-loader',
          // Compiles Sass to CSS
          'sass-loader',
        ],
      },
    ]
  }
};