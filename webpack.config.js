const path = require("path");
const autoprefixer = require('autoprefixer');

// webpack.config.js
module.exports = {
  mode: 'development',
  entry: ["./src/main/webapp/style.scss", "./src/main/webapp/script.js"],
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
        test: /\.scss$/,
        use: [
          {
            loader: 'file-loader',
            options: {
              name: 'bundle.css',
            },
          },
          { loader: 'extract-loader' },
          { loader: 'css-loader' },
          {
            loader: 'postcss-loader',
            options: {
              plugins: () => [autoprefixer()]
            }
          },
          {
            loader: 'sass-loader',
            options: {
              implementation: require('sass'),
              webpackImporter: false,
              sassOptions: {
                includePaths: ['./node_modules'],
              },
            },
          }
        ],
      },
    ]
  }
};