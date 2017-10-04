import babel from "rollup-plugin-babel";
import commonjs from "rollup-plugin-commonjs";
import replace from "rollup-plugin-replace"; 
import resolve from "rollup-plugin-node-resolve"; 
import uglify from 'rollup-plugin-uglify';

// Transpile es7.js version to standard js
export default {
  input: "www/GooglePlaces.js",
  output: {
    file: "www/GooglePlaces.dist.js",
    format: "cjs",
  },
  plugins: [
    babel({
      exclude: "node_modules/**",
    }),
    replace({
      exclude: "node_modules/**",
      "process.env.NODE_ENV": JSON.stringify(process.env.NODE_ENV || "production"),
    }),
    resolve(),
    commonjs(),
    uglify(),
  ],
};

