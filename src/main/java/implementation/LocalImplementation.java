package implementation;

import specifikacija.FileStorageSpecification;
import specifikacija.SpecificationManager;
import specifikacija.izuzeci.*;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class LocalImplementation implements FileStorageSpecification {

    private String separator= File.separator;
    private String storageRoot;
    private String storageIme;
    private String path;
    private String[] restrictions;
    private long maxStorageSize;
    private long currentSize;
    private File storageFile;
    private File metadata;
    private HashMap<String,Integer>directoryMap=new HashMap<>();
    private String currPath;

    private LocalImplementation(){}

    static{
        SpecificationManager.registerImplementation(new LocalImplementation());
    }

    @Override
    public void setFileName(String fileName) {

    }

    @Override
    public void createFile(String fileName) throws InvalidExtension, DuplicateName {
        File newFile = new File(getCurrPath()+separator+fileName);
        HashMap<String,Integer>map2 = getDirectoryMap();
        try {
            int last=getCurrPath().lastIndexOf(separator)+1;
            String ime=getCurrPath().substring(last);
            System.out.println("Pravim fajl u "+ime);
            if(getCurrPath().equalsIgnoreCase(getStorageRoot())){
                int tacka=fileName.lastIndexOf(".")+1;
                String ekst=fileName.substring(tacka);
                boolean flag=false;
                for(int i=0;i<restrictions.length;i++){
                    if(restrictions[i].equalsIgnoreCase(ekst)){
                        flag=true;
                        System.out.println("Ilegalna ekstenzija");
                    }
                }
                if(!flag){
                    newFile.createNewFile();
                    map2.put(getStorageIme(),map2.get(getStorageIme())-1);
                    setDirectoryMap(map2);
                }
                return;
            }
            if(this.directoryMap.get(ime)>0){
                int tacka=fileName.lastIndexOf(".")+1;
                String ekst=fileName.substring(tacka);
               boolean flag=false;
               for(int i=0;i<restrictions.length;i++){
                   if(restrictions[i].equalsIgnoreCase(ekst)){
                       flag=true;
                       System.out.println("Ilegalna ekstenzija");
                   }
               }
               if(!flag){
                   newFile.createNewFile();
                   map2.put(ime,map2.get(ime)-1);
                   map2.put(getStorageIme(),map2.get(getStorageIme())-1);
                   setDirectoryMap(map2);
               }
            }

           else {
                System.out.println("Folder nema mesta!");
                return;
            }
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteFile(String fileName) throws NoSuchFile {
        File file = new File(getCurrPath()+separator+fileName);
        HashMap<String, Integer>map2 = getDirectoryMap();
        if (file.exists()){
            file.delete();
            if (getCurrPath().equalsIgnoreCase(getStorageRoot())){
                map2.put(getStorageIme(), map2.get(getStorageIme())+1);
            }
            else{
                map2.put(getStorageIme(), map2.get(getStorageIme())+1);
                int last=getCurrPath().lastIndexOf(separator)+1;
                String ime=getCurrPath().substring(last);
                map2.put(ime, map2.get(ime)+1);
            }
            setDirectoryMap(map2);
            return;
        }
    }

    @Override
    public void renameFile(String fileName, String newFileName) throws DuplicateName, NoSuchFile {
        File file = new File(getCurrPath()+separator+fileName);
        if (file.exists()){
            File rename = new File(getCurrPath()+separator+newFileName);
            file.renameTo(rename);
        }
    }

    @Override
    public void moveFile(String fileName, String newDirPath) throws DuplicateName, NoSuchFile, OversizeException {
        //Korisnik unosi imeFajla iz trenutnog direktorijuma kao prvi parametar,
        //kao drugi parametar mora uneti relativnu putanju u odnosu na storageRoot
        //npr. ukoliko se fajl premesta u Storages/skladiste1/folder1 korisnik ce uneti folder1 kao drugi parametar.
        File zaMove = new File(getCurrPath()+separator+fileName);
        if (!zaMove.exists()){
            System.out.println("Ne postoji takav fajl!");
            return;
        }
        Path rezultat = null;
        try {

            rezultat = Files.move(Paths.get(zaMove.getPath()), Paths.get(getStorageRoot()+separator+newDirPath+separator+fileName));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (rezultat!=null){
            System.out.println("Uspesno premestanje fajla...");
        } else {
            System.out.println("Neuspesno premestanje fajla...");
        }

    }

    @Override
    public String getFilePath(String fileName) {
        return null;
    }

    @Override
    public boolean downloadFile(String fileName) throws NoSuchFile, UnsupportedOperation {
        return false;//za drive
    }

    @Override
    public boolean uploadFile(String fileName) throws UnsupportedOperation {
        return false;//za drive
    }

    private  String[] parseExt(String s){
        return s.split(",");
    }
    private long calculateCurrSize(File storageFile){
        File[] files = storageFile.listFiles();
        long sum=0;
        for(int i=0;i<files.length;i++){
            if(files[i].isDirectory()){
                sum+=calculateCurrSize(files[i]);
            }
            else
            sum+=files[i].length();
        }
        return sum;
    }

    private void parseDir(String s){
        String[] folderData=s.split(",");
        for(int i=0;i<folderData.length;i++){
            String[] data=folderData[i].split("-");
            directoryMap.put(data[0],Integer.parseInt(data[1]));
        }
    }

    @Override
    public void storageInit(String path, int storageSize, String restriction) {

        File fileStorage=new File("Storages");
        HashMap<String, Integer>map2 = getDirectoryMap();
        if(!fileStorage.isDirectory()){
            fileStorage.mkdir();
        }
        maxStorageSize=storageSize;
        this.storageFile=new File(fileStorage.getPath()+separator+path);
        setStorageRoot(storageFile.getPath()); //podesavanje storageRoot-a, npr. Storages/skladiste1
        if(!storageFile.isDirectory()){
            storageFile.mkdir();
            setCurrPath(storageFile.getPath());
            setStorageIme(path);
            currentSize=0;
            maxStorageSize=storageSize;
           // this.directoryMap.put(this.storageIme,storageSize);
            map2.put(this.storageIme, Integer.valueOf(storageSize));
            setDirectoryMap(map2);
            restrictions = parseExt(restriction);
            metadata=new File(storageFile + separator + "data.meta");
            try {
                metadata.createNewFile();
                BufferedWriter bw=new BufferedWriter(new FileWriter(metadata));
                bw.write(restriction);
                bw.newLine();
                bw.write(maxStorageSize + "");
                bw.close();

            }catch (Exception e){
                e.printStackTrace();
            }
        }
       /* else{
            currentSize=calculateCurrSize(storageFile);
            setCurrPath(storageFile.getPath());
            try {
                BufferedReader br=new BufferedReader(new FileReader(new File(storageFile.getPath()+separator+"data.meta")));
                String line=br.readLine();
                    this.restrictions=parseExt(line.trim());
                    line=br.readLine();
                    this.maxStorageSize=Long.parseLong(line.trim());
                    line=br.readLine();
                    parseDir(line);
                    br.close();

            }catch (Exception e){
                e.printStackTrace();
            }
        }*/


    }



    @Override
    public void createDirectory(String name, Integer... numOfFiles) throws DuplicateName {

        HashMap<String,Integer>map2 = getDirectoryMap();
        File file=new File(getCurrPath()+ separator+ name);
        if(!file.exists()){
            file.mkdir();
        }
        else throw new DuplicateName();
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(this.metadata,true));
            bufferedWriter.newLine();
            if(numOfFiles.length<=0){
                int s=128;
                bufferedWriter.write(name+" : "+ s);
               // this.directoryMap.put(name,s);
                map2.put(name, Integer.valueOf(s));
            }
            else{
                bufferedWriter.write(name+" : "+  numOfFiles[0].toString());
                map2.put(name, Integer.valueOf(numOfFiles[0]));
            }

            if (getCurrPath().equalsIgnoreCase(getStorageRoot())){
                map2.put(getStorageIme(), map2.get(getStorageIme())-1);
            }
            else{
                map2.put(getStorageIme(), map2.get(getStorageIme())-1);
                int last=getCurrPath().lastIndexOf(separator)+1;
                String ime=getCurrPath().substring(last);
                map2.put(ime, map2.get(ime)-1);
            }

            setDirectoryMap(map2);
            bufferedWriter.newLine();
            bufferedWriter.close();

        }catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("Uspesno update-ovan data.meta fajl.");

    }

    private void pomBrisanje(String path){
        File zaBrisanje=new File(path);
        File[] files=zaBrisanje.listFiles();
        HashMap<String,Integer>map2 = getDirectoryMap();
        for(File f:files){
            if(f.isDirectory())pomBrisanje(f.getPath());
            f.delete();
            if (getCurrPath().equalsIgnoreCase(getStorageRoot())){
                map2.put(getStorageIme(), map2.get(getStorageIme())+1);
            }
            else{
                map2.put(getStorageIme(), map2.get(getStorageIme())+1);
                int last=getCurrPath().lastIndexOf(separator)+1;
                String ime=getCurrPath().substring(last);
                map2.put(ime, map2.get(ime)+1);
                setDirectoryMap(map2);
            }
        }
    }
    @Override
    public void deleteDirectory(String dirName) throws NoSuchFile, InvalidDelete {

        File zaBrisanje=new File(getCurrPath()+separator+dirName);
        HashMap<String,Integer>map2 = getDirectoryMap();
        File[] files=zaBrisanje.listFiles();
        for(File f:files){
            if(f.isDirectory())pomBrisanje(f.getPath());
            f.delete();
        }
        zaBrisanje.delete();
        for (int i =0;i<files.length;i++){
            if (getCurrPath().equalsIgnoreCase(getStorageRoot())){
                map2.put(getStorageIme(), map2.get(getStorageIme())-1);
            }
            else{
                map2.put(getStorageIme(), map2.get(getStorageIme())-1);
                int last=getCurrPath().lastIndexOf(separator)+1;
                String ime=getCurrPath().substring(last);
                map2.put(ime, map2.get(ime)+1);
            }
        }
        setDirectoryMap(map2);
    }

    @Override
    public void setUnsupportedExtensions(String[] unsupportedExtensions) {

    }

    @Override
    public String getDirectoryPath(String name) {
        return null;
    }

    @Override
    public void downoloadDirectory(String sourcePath, String destinationPath) throws UnsupportedOperation {
        //ovo je za drive
    }

    @Override
    public void renameDirectory(String dirName, String novoIme) throws UnsupportedOperation, DuplicateName {
        try {
            HashMap<String,Integer>map2 = getDirectoryMap();
            map2.put(novoIme, map2.get(dirName));
            map2.remove(dirName);
            renameFile(dirName,novoIme);
        } catch (NoSuchFile e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> listAll(String... path) { //ls [home]
       // System.out.println(getTrenutniPath()+" je trenutna putanja");
        List<File> lista = new ArrayList<>();
        List<String>zaRet = new ArrayList<>();
        //File file = new File(getTrenutniPath());
        File file;
        System.out.println(path.length);
        if (path.length<=0){
            file = new File(getCurrPath());
        } else {
            String putanja = getStorageRoot()+separator+path[0];
            System.out.println("Radim ls za "+putanja);
            file = new File(putanja);
        }
        File[] files = file.listFiles();

        if (files!=null){
            for (File f:files){
                //System.out.println(f.getName());
                lista.add(f);
                zaRet.add(f.getName());
            }
        }
       // pomZaIspis(lista);
        return zaRet;
    }

    private List<File>pomFull(String path, List<File> lista){
        File file=new File(path);
        // lista= Arrays.asList(file.list());
        for (File f:file.listFiles()){
            if (!lista.contains(f))
                lista.add(f);
        }
        File[] files=file.listFiles();
        for(File f:files){
            if(f.isDirectory())lista.addAll(pomFull(f.getPath(), lista));
        }

        return lista;
    }

    @Override
    public List<String> listFull(String... path) {
        //rekurzivno pozvati listAll
        List<String>lista=new ArrayList<>();
        List<String> pomList = new ArrayList<>();
        List<File>ispis = new ArrayList<>();
        if(path.length==0){
            File file=new File(getCurrPath());
            ispis.addAll(List.of(file.listFiles()));
            //lista= Arrays.asList(Objects.requireNonNull(file.list())); //fiksira se velicina za lista
           // pomList.addAll(lista);
            File[] files=file.listFiles();
            for(File f:files){
                if(f.isDirectory())
                    ispis.addAll(pomFull(f.getPath(), ispis));
            }

        }
        else{
            File file=new File(getCurrPath()+separator+path[0]);
            File curr = new File(getCurrPath());
            if (file.getParentFile()!=curr){
                return null;
            }
            //lista= Arrays.asList(Objects.requireNonNull(file.list()));
            //pomList.addAll(lista);
            ispis.addAll(List.of(file.listFiles()));
            File[] files=file.listFiles();
            for(File f:files){
                if(f.isDirectory())
                    ispis.addAll(pomFull(f.getPath(), ispis));
            }
        }


        List<String> listWithoutDuplicates = pomList.stream()
                .distinct()
                .collect(Collectors.toList());
        System.out.println(listWithoutDuplicates);
        List<File> listWithoutDuplicates2 = ispis.stream()
                .distinct()
                .collect(Collectors.toList());
        System.out.println(listWithoutDuplicates);
        List<String>zaRet = new ArrayList<>();
        for (File f:listWithoutDuplicates2){
            zaRet.add(f.getName());
        }

       // pomZaIspis(listWithoutDuplicates2);

        return zaRet;
    }




    @Override
    public List<String> listExt(String... extname) {
        //Korisnik salje ekstenzije odvojene po zarezima, pretrazuje se u okviru trenutnog direktorijuma samo
        List<String>filePaths = new ArrayList<>();
        List<String>ekstenzije = Arrays.asList(extname);
        //  System.out.println(ekstenzije);
        // System.out.println(extname.length-1);
        File trenutniDir = new File(getCurrPath());
        List<File>files = new ArrayList<>();
        for (File file : trenutniDir.listFiles()){
            for (int i=0;i<ekstenzije.size();i++){
                if (!file.isDirectory() && file.getName().contains(ekstenzije.get(i))){
                    filePaths.add(file.getPath());
                    files.add(file);
                }
            }
        }
       // pomZaIspis(files);
        List<String>zaRet = new ArrayList<>();
        for (File f:files){
            zaRet.add(f.getName());
        }
        return zaRet;
    }

    @Override
    public List<String> listFilesWith(String substring) {
        //radi za trenutni direktorijum!
        File currDir = new File(getCurrPath());
        List<File>files = List.of(currDir.listFiles());
        List<File>ispis = new ArrayList<>();
        for (File file : files){
            if (file.getName().contains(substring)){
                ispis.add(file);
            }
        }
       // pomZaIspis(ispis);
        List<String>zaRet = new ArrayList<>();
        for (File f:ispis){
            zaRet.add(f.getName());
        }
        return zaRet;
    }

    @Override
    public boolean listContains(String path, String... fileNames) {
        //fileNames MORAJU BITI ODVOJENI ZAREZOM ukoliko ih je vise
        File dirZaPretragu = new File(getCurrPath()+separator+path); //korisnik unese direktorijum koji zeli da pretrazi
        boolean sadrzi = true;
        List<String>fajlovi = Arrays.asList(fileNames);
        int brojacFajlova = 0;
        for (File file : dirZaPretragu.listFiles()){
            if (fajlovi.contains(file.getName())){
                brojacFajlova++;
            }
        }
        if (fajlovi.size()==brojacFajlova){
            System.out.println("Direktorijum: "+path +" sadrzi prosledjene fajlove.");
            return true;
        }
        System.out.println("Direktorijum: "+path +" NE sadrzi prosledjene fajlove.");

        return false;
    }


    //Pomocna funkcija za findDirectory koja rekurzivno prolazi kroz
    //poddirektorijume i trazi fajl.
    private String pomFindDir(File dir, String trazeniFajl){
        for (File file: dir.listFiles()){
            if (file.isDirectory()){
                return pomFindDir(file, trazeniFajl);
            }
            if (file.getName().equalsIgnoreCase(trazeniFajl)){
                return file.getParent();
            }
        }
        return "Nije pronadjen fajl.";
    }
    @Override
    public String findDirecotry(String fileName) throws NoSuchFile {
        //Nalazi direktorijum u kome je fajl.
        File storage = new File(getStorageRoot());
        for (File f : storage.listFiles()){
            if (f.isDirectory()){
                return pomFindDir(f, fileName);
            }
            if (f.getName().equalsIgnoreCase(fileName)){
                return f.getParent();
            }
        }
        return "Nije pronadjen fajl.";
    }


    private List<File> dodaj(File file){
        List<File>zaPov=new ArrayList<>();
        File[] files=file.listFiles();
        for(File f:files){
            if(f.isDirectory()){
                zaPov.add(f);
                zaPov.addAll(dodaj(f));
            }
            else zaPov.add(f);
        }
        return zaPov;
    }

    @Override
    public List<String> sort(String order, String... criteria) {

        List<File>zaSort=new ArrayList<>();
        File[] files= storageFile.listFiles();
        for(File f:files){
            if(f.isDirectory()){
                zaSort.add(f);
                zaSort.addAll(dodaj(f));

            }
            else zaSort.add(f);
        }
        File[] s= new File[zaSort.size()];
        s= (File[]) zaSort.toArray(s);
        Arrays.sort(s, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if(criteria[0].equalsIgnoreCase("name"))
                    return (o1.getName().compareTo(o2.getName()));
                if(criteria[0].equalsIgnoreCase("date")){
                    BasicFileAttributes attr1,attr2;
                    try {
                        attr1=Files.readAttributes(o1.toPath(),BasicFileAttributes.class);
                        attr2=Files.readAttributes(o2.toPath(),BasicFileAttributes.class);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return (attr1.creationTime().compareTo(attr2.creationTime()));
                }
                if(criteria[0].equalsIgnoreCase("size")){
                    long s1,s2;
                    try {
                        s1=Files.size(o1.toPath());
                        s2=Files.size(o2.toPath());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return Long.compare(s1,s2);
                }
                return 0;
            }
        });
        if(order.equalsIgnoreCase("desc"))Collections.reverse(Arrays.asList(s));

        List<String>zaRet = new ArrayList<>();

        for(int i=0;i<s.length;i++){
            zaRet.add(s[i].getName());
        }
        //pomZaIspis(List.of(s));
        return zaRet;
    }

    private File pomFindDir2(String trazeniDir,File trenutniDir){
        for (File file : trenutniDir.listFiles()){
            if(file.isDirectory()&&file.getName().equalsIgnoreCase(trazeniDir))return file;
            if (file.isDirectory()){
                return pomFindDir2(trazeniDir,file);
            }

        }
        return null;
    }

    @Override
    public List<String> filesFromPeriod(String path, String time) {

        String[] datumi=time.split("-");

        Date date1;
        Date date2;
        File dir= pomFindDir2(path,storageFile);
        ArrayList<File>press=new ArrayList<>();
        List<String>ret = new ArrayList<>();
        try {
            date1=new SimpleDateFormat("dd/MM/yyyy").parse(datumi[0]);
            date2=new SimpleDateFormat("dd/MM/yyyy").parse(datumi[1]);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        File[] files=dir.listFiles();
        for(File f:files){
            long lastModified = f.lastModified();
            Date d = new Date(lastModified);
            System.out.println(f.getName()+ " " + d);
            if(d.after(date1)&&d.before(date2)){
                press.add(f);
            }
        }

        for (File f : press){
            ret.add(f.getName());
        }
       // pomZaIspis(press);
        return ret;
    }

    boolean fName = true;
    boolean fSize;
    boolean fPath;
    boolean fDate;
    @Override
    public void fileInfoFilter(String... modifications) {
        int i = modifications.length-1;

        fSize = false;
        fPath = false;
        fDate = false;

        for (int j =0;j<=i;j++){
            if (modifications[j].equalsIgnoreCase("name"))
                fName = true;
            if (modifications[j].equalsIgnoreCase("size"))
                fSize = true;
            if (modifications[j].equalsIgnoreCase("path"))
                fPath = true;
            if (modifications[j].equalsIgnoreCase("date"))
                fDate = true;
        }


    }

    private void pomZaIspis(List<File>filesList){
        List<File>files = filesList;
        for (File file : files){
            if (fName){
                System.out.print("Name: "+file.getName()+ " | ");
                if (file.isDirectory()){
                    System.out.print("Remaining space: "+getDirectoryMap().get(file.getName())+ " | ");
                }
            }
            if (fSize) {
                Path path = Paths.get(file.getPath());

                try {

                    // size of a file (in bytes)
                    long bytes = Files.size(path);
                    System.out.print("Size: " + bytes + " | ");

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            if (fPath)
                System.out.print("Path: "+ file.getPath()+ " | ");
            if (fDate) {
                try {

                    Path filePath = Paths.get(file.getPath());
                    BasicFileAttributes attr =
                            Files.readAttributes(filePath, BasicFileAttributes.class);

                    //  System.out.println("creationTime: " + attr.creationTime());
                    // System.out.println("lastAccessTime: " + attr.lastAccessTime());
                    // System.out.println("lastModifiedTime: " + attr.lastModifiedTime());
                    System.out.print("Date: " + attr.creationTime()+ " | ");

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            System.out.println();
        }
    }


    @Override
    public boolean forward(String path) { //cd Luka
        File currentDir = new File(getCurrPath());
        List<File>podDirs = List.of(currentDir.listFiles());
        for (File podDir : podDirs){
            if (podDir.getName().contains(path) && podDir.isDirectory()){
                setCurrPath(podDir.getPath());
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean backwards() { //cd ..
        File current = new File(getCurrPath());
        if (!getCurrPath().equalsIgnoreCase(getStorageFile().getPath()) && current.getParentFile().exists()){
            File dirParent = current.getParentFile();
            setCurrPath(dirParent.getPath());
        }
        return false;
    }

    @Override
    public String getRoot() {
        return null;
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public String getStorageRoot() {
        return storageRoot;
    }

    public void setStorageRoot(String storageRoot) {
        this.storageRoot = storageRoot;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String[] getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(String[] restrictions) {
        this.restrictions = restrictions;
    }

    public long getMaxStorageSize() {
        return maxStorageSize;
    }

    public void setMaxStorageSize(long maxStorageSize) {
        this.maxStorageSize = maxStorageSize;
    }

    public long getCurrentSize() {
        return currentSize;
    }

    public void setCurrentSize(long currentSize) {
        this.currentSize = currentSize;
    }

    public File getStorageFile() {
        return storageFile;
    }

    public void setStorageFile(File storageFile) {
        this.storageFile = storageFile;
    }

    public File getMetadata() {
        return metadata;
    }

    public void setMetadata(File metadata) {
        this.metadata = metadata;
    }

    public HashMap<String, Integer> getDirectoryMap() {
        return directoryMap;
    }

    public void setDirectoryMap(HashMap<String, Integer> directoryMap) {
        this.directoryMap = directoryMap;
    }

    public String getCurrPath() {
        return currPath;
    }

    public void setCurrPath(String currPath) {
        this.currPath = currPath;
    }

    public String getStorageIme() {
        return storageIme;
    }

    public void setStorageIme(String storageIme) {
        this.storageIme = storageIme;
    }
}

/* -----Drugacija implementacija za filter metodu,
        Preko booleana i pomocne funkcije za ispis
        private void pomZaIspis(List<String>filesList){
        List<File>files = new ArrayList<>();
        for(String s:filesList){
            File f = new File(s);
            files.add(f);
        }
        for (File file : files){
            if (fName)
                System.out.print("Name: "+file.getName()+ " | ");
            if (fSize) {
                Path path = Paths.get(file.getPath());

                try {

                    // size of a file (in bytes)
                    long bytes = Files.size(path);
                    System.out.print("Size: " + bytes + " | ");

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            if (fPath)
                System.out.print("Path: "+ file.getPath()+ " | ");
            if (fDate) {
                try {

                    Path filePath = Paths.get(file.getPath());
                    BasicFileAttributes attr =
                            Files.readAttributes(filePath, BasicFileAttributes.class);

                  //  System.out.println("creationTime: " + attr.creationTime());
                   // System.out.println("lastAccessTime: " + attr.lastAccessTime());
                   // System.out.println("lastModifiedTime: " + attr.lastModifiedTime());
                    System.out.print("Date: " + attr.creationTime()+ " | ");

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            System.out.println();
        }
    }

    boolean fName = true;
    boolean fSize;
    boolean fPath;
    boolean fDate;
    @Override
    public void fileInfoFilter(String... modifications) {
        int i = modifications.length-1;

        fSize = false;
        fPath = false;
        fDate = false;

       for (int j =0;j<=i;j++){
           if (modifications[j].equalsIgnoreCase("name"))
               fName = true;
           if (modifications[j].equalsIgnoreCase("size"))
               fSize = true;
           if (modifications[j].equalsIgnoreCase("path"))
               fPath = true;
           if (modifications[j].equalsIgnoreCase("date"))
               fDate = true;
       }


    }


 */