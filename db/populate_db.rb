DBNAME = 'vaffapp.db'
TBL_NAME = 'insults'
ins_arr = []
id_region = nil
insults_per_region = 0

region = Hash.new(0)

region = {
  "molise" => 1,
  "valle-d'aosta" => 2,
  "piemonte" => 3,
  "lombardia" => 4,
  "trentino-alto-adige" => 5,
  "friuli-venezia-giulia" => 6,
  "veneto" => 7,
  "emilia-romagna" => 8,
  "liguria" => 9,
  "toscana" => 10,
  "lazio" => 11,
  "umbria" => 12,
  "marche" => 13,
  "abruzzo" => 14,
  "campania" => 15,
  "puglia" => 16,
  "basilicata" => 17,
  "calabria" => 18,
  "sicilia" => 19,
  "sardegna" => 20,
}  

#l = ->(str){ str.gsub("\n",'') }  

# begin/rescue in case file doesn't exist
begin
  File.open("insulti.txt", "r") do |infile|
    while (line = infile.gets)
      next if line[0] == "\n"
      if line[0] == '#'
        if line[1] == '#'
          # print number of insults from previous region, then reset
          puts "#{insults_per_region} insulti" if insults_per_region > 0
          insults_per_region = 0

          line = line[2..-1]
          reg = line.gsub!(/\s+/, '').downcase
          id_region = region[reg] 
          print "#{reg} "
          next
        else
          next
        end
      end
      insults_per_region+=1
      virgola = line.index(',')
      ins_arr << [ line[0...virgola], line[virgola+1..-1].gsub("\n",''), id_region ]
    end
  end
rescue Exception => e
 puts e
end

# print number of insults from last region
puts "#{insults_per_region} insulti" if insults_per_region > 0

ins_arr.each do |a|
  sql = "insert into #{TBL_NAME} values (#{a[0]},#{a[1]},#{a[2]})"
  %x( sqlite3 #{DBNAME} "#{sql}" )
  unless $?.exitstatus == 0
    puts sql
    puts $?
  end
end
