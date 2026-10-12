/**
 * Saves a Blob to the user's download directory under the given name, through
 * a temporary object URL on a temporary link.
 * @param {Blob} blob - What to save.
 * @param {string} filename - The name to save it under.
 * @returns {void}
 */
export function saveBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  // Revoke on the next task: revoking synchronously can cancel the download in
  // some browsers before it has started reading the Blob.
  setTimeout(() => URL.revokeObjectURL(url), 0);
}
