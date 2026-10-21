import reactHooks from "eslint-plugin-react-hooks";

// Solo las reglas de hooks: el lint está para validar el uso de states y efectos.
export default [
  { ignores: ["dist/"] },
  {
    files: ["src/**/*.{js,jsx}"],
    languageOptions: { parserOptions: { ecmaFeatures: { jsx: true } } },
    ...reactHooks.configs.flat.recommended,
  },
];
