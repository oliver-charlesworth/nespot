// Default config doesn't work for workers.
// See e.g. https://medium.com/@JakeXiao/window-is-undefined-in-umd-library-output-for-webpack4-858af1b881df
config.output.globalObject = "typeof self !== 'undefined' ? self : this";

config.devServer.historyApiFallback = true;
