public class Utilisateur {
 private String nom;
 private String adresseIP;

 public Utilisateur(String nom) {
  this.nom = nom;
 }

 public String getNom() {
  return nom;
 }

 public void setNom(String nom) {
  this.nom = nom;
 }

 public String getAdresseIP() {
  return adresseIP;
 }

 public void setAdresseIP(String adresseIP) {
  this.adresseIP = adresseIP;
 }

 @Override
 public String toString() {
  return "Utilisateur{nom='" + nom + "', adresseIP='" + adresseIP + "'}";
 }
}